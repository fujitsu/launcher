/*
 * Copyright 2017 Red Hat, Inc.
 * Copyright 2021-2023 Fujitsu Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.core.timer;

import static io.smallrye.faulttolerance.core.timer.TimerLogger.LOG;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import io.smallrye.faulttolerance.core.util.RunnableWrapper;

/**
 * Starts one thread that processes submitted tasks in a loop and when it's time for a task to run,
 * it gets submitted to the executor. The default executor is provided by a caller, so the caller
 * must shut down this timer <em>before</em> shutting down the executor.
 */
// TODO implement a hashed wheel?
public final class ThreadTimer implements Timer {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final Comparator<Task> TASK_COMPARATOR = (o1, o2) -> {
        if (o1 == o2) {
            // two different instances are never equal
            return 0;
        }

        // must _not_ return 0 if start times are equal, because that isn't consistent
        // with `equals` (see also above)
        return o1.startTime <= o2.startTime ? -1 : 1;
    };

    private final String name;

    private final SortedSet<Task> tasks;

    private final Thread thread;

    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * @param defaultExecutor default {@link Executor} used for running scheduled tasks, unless an executor
     *        is provided when {@linkplain #schedule(long, Runnable, Executor) scheduling} a task
     * @param factory
     */
    public ThreadTimer(Executor defaultExecutor, ThreadFactory factory) {
        checkNotNull(defaultExecutor, "Executor must be set");

        this.name = "SmallRye Fault Tolerance Timer " + COUNTER.incrementAndGet();
        LOG.createdTimer(name);

        this.tasks = new ConcurrentSkipListSet<>(TASK_COMPARATOR);
        this.thread = factory.newThread(() -> {
            while (running.get()) {
                try {
                    if (tasks.isEmpty()) {
                        LockSupport.park();
                    } else {
                        Task task;
                        try {
                            task = tasks.first();
                        } catch (NoSuchElementException e) {
                            // can happen if all tasks are cancelled right between `tasks.isEmpty` and `tasks.first`
                            continue;
                        }

                        long currentTime = System.nanoTime();
                        long taskStartTime = task.startTime;

                        // must _not_ use `taskStartTime <= currentTime`, because `System.nanoTime()`
                        // is relative to an arbitrary number and so it can possibly overflow;
                        // in such case, `taskStartTime` can be positive, `currentTime` can be negative,
                        //  and yet `taskStartTime` is _before_ `currentTime`
                        if (taskStartTime - currentTime <= 0) {
                            tasks.remove(task);
                            if (task.state.compareAndSet(Task.STATE_NEW, Task.STATE_RUNNING)) {
                                Executor executorForTask = task.executorOverride;
                                if (executorForTask == null) {
                                    executorForTask = defaultExecutor;
                                }

                                executorForTask.execute(() -> {
                                    LOG.runningTimerTask(task);
                                    try {
                                        task.runnable.run();
                                    } finally {
                                        task.state.set(Task.STATE_FINISHED);
                                    }
                                });
                            }
                        } else {
                            // this is OK even if another timer is scheduled during the sleep (even if that timer should
                            // fire sooner than `taskStartTime`), because `schedule` always calls` LockSupport.unpark`
                            LockSupport.parkNanos(taskStartTime - currentTime);
                        }
                    }
                } catch (Exception e) {
                    // can happen e.g. when the executor is shut down sooner than the timer
                    LOG.unexpectedExceptionInTimerLoop(e);
                }
            }
        });
        thread.start();
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task) {
        return schedule(delayInMillis, task, null);
    }

    @Override
    public TimerTask schedule(long delayInMillis, Runnable task, Executor executor) {
        long startTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delayInMillis);
        Task timerTask = new Task(startTime, RunnableWrapper.INSTANCE.wrap(task), tasks::remove, executor);
        tasks.add(timerTask);
        LockSupport.unpark(thread);
        LOG.scheduledTimerTask(timerTask, delayInMillis);
        return timerTask;
    }

    @Override
    public void shutdown() throws InterruptedException {
        if (running.compareAndSet(true, false)) {
            LOG.shutdownTimer(name);
            thread.interrupt();
            thread.join();
        }
    }

    private static final class Task implements TimerTask {
        static final int STATE_NEW = 0; // was scheduled, but isn't running yet
        static final int STATE_RUNNING = 1; // running on the executor
        static final int STATE_FINISHED = 2; // finished running
        static final int STATE_CANCELLED = 3; // cancelled before it could be executed

        final long startTime; // in nanos, to be compared with System.nanoTime()
        final Runnable runnable;
        final Executor executorOverride; // may be null, which means that the timer's executor shall be used
        final AtomicInteger state = new AtomicInteger(STATE_NEW);

        private final Consumer<TimerTask> onCancel;

        Task(long startTime, Runnable runnable, Consumer<TimerTask> onCancel, Executor executorOverride) {
            this.startTime = startTime;
            this.runnable = checkNotNull(runnable, "Runnable task must be set");
            this.onCancel = checkNotNull(onCancel, "Cancellation callback must be set");
            this.executorOverride = executorOverride;
        }

        @Override
        public boolean isDone() {
            int state = this.state.get();
            return state == STATE_FINISHED || state == STATE_CANCELLED;
        }

        @Override
        public boolean cancel() {
            // can't cancel if it's already running
            if (state.compareAndSet(STATE_NEW, STATE_CANCELLED)) {
                LOG.cancelledTimerTask(this);
                onCancel.accept(this);
                return true;
            }
            return false;
        }
    }
}