package com.bookmap.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Simple implementation of instrumentation for the measurement of the code's execution time
 */
public class StopWatch {

  public static final double NANOSECONDS = 1000_000_000;
  private final ThreadMXBean threadTimer = ManagementFactory.getThreadMXBean();
  private long start = threadTimer.getCurrentThreadCpuTime();

  public double elapsed() {
    return (threadTimer.getCurrentThreadCpuTime() - start) / NANOSECONDS;
  }
  public void reset() {
    start = threadTimer.getCurrentThreadCpuTime();
  }

}
