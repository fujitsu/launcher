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
package io.smallrye.faulttolerance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.ThreadTimer;
import io.smallrye.faulttolerance.core.timer.Timer;

import com.fujitsu.launcher.microprofile.faulttolerance.ThreadFactoryProvider;

@Singleton
public class ExecutorHolder {
    private final ExecutorService asyncExecutor;

    private final EventLoop eventLoop;

    private final Timer timer;

    private final boolean shouldShutdownAsyncExecutor;

    @Inject
    public ExecutorHolder(AsyncExecutorProvider asyncExecutorProvider, ThreadFactoryProvider threadFactoryProvider) {
        this.asyncExecutor = asyncExecutorProvider.get();
        this.eventLoop = EventLoop.get();
        this.timer = new ThreadTimer(asyncExecutor, threadFactoryProvider.get());
        this.shouldShutdownAsyncExecutor = asyncExecutorProvider instanceof DefaultAsyncExecutorProvider;
    }

    @PreDestroy
    public void tearDown() {
        try {
            timer.shutdown();
        } catch (InterruptedException e) {
            // no need to do anything, we're shutting down anyway
            // just set the interruption flag to be a good citizen
            Thread.currentThread().interrupt();
        }

        if (shouldShutdownAsyncExecutor) {
            asyncExecutor.shutdownNow();
            try {
                asyncExecutor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // no need to do anything, we're shutting down anyway
                // just set the interruption flag to be a good citizen
                Thread.currentThread().interrupt();
            }
        }
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    public EventLoop getEventLoop() {
        return eventLoop;
    }

    public Timer getTimer() {
        return timer;
    }
}
