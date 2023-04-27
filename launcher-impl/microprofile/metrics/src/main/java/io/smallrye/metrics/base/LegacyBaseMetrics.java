/*
 * Copyright (c) 2023 Fujitsu Limited and/or its affiliates. All rights
 * reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This file incorporates work authored by SmallRye Metrics,
 * licensed under the Apache License, Version 2.0, which is available at
 * http://www.apache.org/licenses/LICENSE-2.0.
 */

package io.smallrye.metrics.base;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;

import io.micrometer.core.instrument.binder.BaseUnits;
import io.smallrye.metrics.legacyapi.LegacyMetricRegistryAdapter;

/**
 * Base metrics from the MP Metrics 3.0/4.0 spec.
 */
public class LegacyBaseMetrics {

    // If we are running with GraalVM native mode, some metrics are not supported
    // and will be skipped
    private final boolean nativeMode;

    private static final String GARBAGE_COLLECTION_TOTAL = "gc.total";
    private static final String GARBAGE_COLLECTION_TIME = "gc.time";
    private static final String THREAD_COUNT = "thread.count";
    private static final String THREAD_DAEMON_COUNT = "thread.daemon.count";
    private static final String THREAD_MAX_COUNT = "thread.max.count";
    private static final String CURRENT_LOADED_CLASS_COUNT = "classloader.loadedClasses.count";
    private static final String TOTAL_LOADED_CLASS_COUNT = "classloader.loadedClasses.total";
    private static final String TOTAL_UNLOADED_CLASS_COUNT = "classloader.unloadedClasses.total";
    private static final String JVM_UPTIME = "jvm.uptime";
    private static final String SYSTEM_LOAD_AVERAGE = "cpu.systemLoadAverage";
    private static final String CPU_AVAILABLE_PROCESSORS = "cpu.availableProcessors";
//    private static final String PROCESS_CPU_LOAD = "cpu.processCpuLoad";
//    private static final String PROCESS_CPU_TIME = "cpu.processCpuTime";
    private static final String MEMORY_COMMITTED_HEAP = "memory.committedHeap";
    private static final String MEMORY_MAX_HEAP = "memory.maxHeap";
    private static final String MEMORY_USED_HEAP = "memory.usedHeap";

    public LegacyBaseMetrics() {
        this.nativeMode = false;
    }

    public LegacyBaseMetrics(boolean nativeMode) {
        this.nativeMode = nativeMode;
    }

    public void register(MetricRegistry registry) {
        garbageCollectionMetrics(registry);
        classLoadingMetrics(registry);
        baseOperatingSystemMetrics(registry);
        threadingMetrics(registry);
        runtimeMetrics(registry);
        baseMemoryMetrics(registry);
    }

    private void garbageCollectionMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {

                LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;
                Tag tag = new Tag("name", gc.getName());

                Metadata metadata = Metadata.builder().withName(GARBAGE_COLLECTION_TOTAL)
                        .withDescription("Displays the total number of collections that have occurred."
                                + " This attribute lists -1 if the collection count is undefined for this collector.")
                        .build();
                lmr.counter(metadata, gc, GarbageCollectorMXBean::getCollectionCount, new Tag[] { tag });

                /*
                 * Need to convert from milliseconds to seconds.
                 */
                metadata = Metadata.builder().withName(GARBAGE_COLLECTION_TIME)
                        .withDescription(
                                "Displays the approximate accumulated collection elapsed time in seconds. This attribute "
                                        + "displays -1 if the collection elapsed time is undefined for this collector. The Java "
                                        + "virtual machine implementation may use a high resolution timer to measure the "
                                        + "elapsed time. This attribute may display the same value even if the collection "
                                        + "count has been incremented if the collection elapsed time is very short.")
                        .withUnit(MetricUnits.SECONDS).build();
                lmr.counter(metadata, gc, gcObj -> (gcObj.getCollectionTime() / 1e+3), new Tag[] { tag });

            }
        }
    }

    private void classLoadingMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
            LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;
            Metadata metadata = Metadata.builder().withName(TOTAL_LOADED_CLASS_COUNT).withDescription(
                            "Displays the total number of classes that have been loaded since the Java virtual machine has started execution.")
                    .build();
            lmr.counter(metadata, classLoadingMXBean, ClassLoadingMXBean::getTotalLoadedClassCount);

            metadata = Metadata.builder().withName(TOTAL_UNLOADED_CLASS_COUNT).withDescription(
                            "Displays the total number of classes unloaded since the Java virtual machine has started execution.")
                    .build();
            lmr.counter(metadata, classLoadingMXBean, ClassLoadingMXBean::getUnloadedClassCount);

            metadata = Metadata.builder().withName(CURRENT_LOADED_CLASS_COUNT).withDescription(
                    "Displays the number of classes that are currently loaded in the Java virtual machine.").build();
            lmr.gauge(metadata, classLoadingMXBean, ClassLoadingMXBean::getLoadedClassCount);
        }
    }

    private void baseOperatingSystemMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;

            Metadata metadata = Metadata.builder().withName(SYSTEM_LOAD_AVERAGE)
                    .withDescription("Displays the system load average for the last minute. The system load average "
                            + "is the sum of the number of runnable entities queued to the available processors and the "
                            + "number of runnable entities running on the available processors averaged over a period of time. "
                            + "The way in which the load average is calculated is operating system specific but is typically a "
                            + "damped time-dependent average. If the load average is not available, a negative value is displayed. "
                            + "This attribute is designed to provide a hint about the system load and may be queried frequently. "
                            + "The load average may be unavailable on some platforms where it is expensive to implement this method.")
                    .build();
            lmr.gauge(metadata, operatingSystemMXBean::getSystemLoadAverage);

            metadata = Metadata.builder().withName(CPU_AVAILABLE_PROCESSORS).withDescription(
                            "Displays the number of processors available to the Java virtual machine. This value may change during "
                                    + "a particular invocation of the virtual machine.")
                    .build();
            lmr.gauge(metadata, operatingSystemMXBean::getAvailableProcessors);

            // some metrics are only available in jdk internal class
            // 'com.sun.management.OperatingSystemMXBean': cast to it.
            // com.sun.management.OperatingSystemMXBean is not available in SubstrateVM
            // the cast will fail for some JVM not derived from HotSpot (J9 for example) so
            // we check if it is assignable to it
//            if (!nativeMode
//                    && com.sun.management.OperatingSystemMXBean.class.isAssignableFrom(operatingSystemMXBean.getClass())) {
//                try {
//                    com.sun.management.OperatingSystemMXBean internalOperatingSystemMXBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
//                    metadata = Metadata.builder().withName(PROCESS_CPU_LOAD)
//                            .withDescription("Displays  the \"recent cpu usage\" for the Java Virtual Machine process. "
//                                    + "This value is a double in the [0.0,1.0] interval. A value of 0.0 means that none of "
//                                    + "the CPUs were running threads from the JVM process during the recent period of time "
//                                    + "observed, while a value of 1.0 means that all CPUs were actively running threads from "
//                                    + "the JVM 100% of the time during the recent period being observed. Threads from the JVM "
//                                    + "include the application threads as well as the JVM internal threads. "
//                                    + "All values betweens 0.0 and 1.0 are possible depending of the activities going on in "
//                                    + "the JVM process and the whole system. "
//                                    + "If the Java Virtual Machine recent CPU usage is not available, the method returns a negative value.")
//                            .withUnit(MetricUnits.PERCENT).build();
//                    lmr.gauge(metadata, internalOperatingSystemMXBean::getProcessCpuLoad);
//
//                    /*
//                     * Must convert from nanoseconds to seconds.
//                     */
//                    metadata = Metadata.builder().withName(PROCESS_CPU_TIME).withDescription(
//                                    "Displays the CPU time used by the process on which the Java virtual machine is running in seconds.")
//                            .withUnit(MetricUnits.SECONDS).build();
//                    lmr.gauge(metadata, () -> (internalOperatingSystemMXBean.getProcessCpuTime() / 1e+9));
//                } catch (ClassCastException ignored) {
//                }
//            }
        }
    }

    private void threadingMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            ThreadMXBean thread = ManagementFactory.getThreadMXBean();
            LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;

            Metadata metadata = Metadata.builder().withName(THREAD_COUNT).withDescription(
                    "Displays the current number of live threads including both daemon and non-daemon threads").build();
            lmr.gauge(metadata, thread::getThreadCount);

            metadata = Metadata.builder().withName(THREAD_DAEMON_COUNT)
                    .withDescription("Displays the current number of live daemon threads.").build();
            lmr.gauge(metadata, thread::getDaemonThreadCount);

            metadata = Metadata.builder().withName(THREAD_MAX_COUNT)
                    .withDescription(
                            "Displays the peak live thread count since the Java virtual machine started or peak was "
                                    + "reset. This includes daemon and non-daemon threads.")
                    .build();
            lmr.gauge(metadata, thread::getPeakThreadCount);
        }
    }

    private void runtimeMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            /*
             * Need to convert from milliseconds to seconds.
             */
            LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;

            Metadata metadata = Metadata.builder().withName(JVM_UPTIME)
                    .withDescription("Displays the time from the start of the Java virtual machine in seconds.")
                    .withUnit(MetricUnits.SECONDS).build();
            lmr.gauge(metadata, () -> (runtimeMXBean.getUptime() / 1e+3));
        }
    }

    private void baseMemoryMetrics(MetricRegistry registry) {
        if (registry instanceof LegacyMetricRegistryAdapter) {
            final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            LegacyMetricRegistryAdapter lmr = (LegacyMetricRegistryAdapter) registry;

            Metadata metadata = Metadata.builder().withName(MEMORY_COMMITTED_HEAP)
                    .withDescription(
                            "Displays the amount of memory in bytes that is committed for the Java virtual machine to use. "
                                    + "This amount of memory is guaranteed for the Java virtual machine to use.")
                    .withUnit(BaseUnits.BYTES).build();
            lmr.gauge(metadata, () -> memoryMXBean.getHeapMemoryUsage().getCommitted());

            metadata = Metadata.builder().withName(MEMORY_MAX_HEAP).withDescription(
                            "Displays the maximum amount of heap memory in bytes that can be used for memory management. "
                                    + "This attribute displays -1 if the maximum heap memory size is undefined. This amount of memory is not "
                                    + "guaranteed to be available for memory management if it is greater than the amount of committed memory. "
                                    + "The Java virtual machine may fail to allocate memory even if the amount of used memory does "
                                    + "not exceed this maximum size.")
                    .withUnit(BaseUnits.BYTES).build();
            lmr.gauge(metadata, () -> memoryMXBean.getHeapMemoryUsage().getMax());

            metadata = Metadata.builder().withName(MEMORY_USED_HEAP)
                    .withDescription("Displays the amount of used heap memory in bytes.").withUnit(BaseUnits.BYTES).build();
            lmr.gauge(metadata, () -> memoryMXBean.getHeapMemoryUsage().getUsed());
        }
    }
}