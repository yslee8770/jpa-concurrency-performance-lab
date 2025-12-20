package com.example.jpa_concurrency_performance_lab.measure;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Component
public class HeapProbe {
    private final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();

    public long usedHeapBytes() {
        return mxBean.getHeapMemoryUsage().getUsed();
    }
}
