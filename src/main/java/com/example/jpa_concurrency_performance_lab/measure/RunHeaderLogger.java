package com.example.jpa_concurrency_performance_lab.measure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RunHeaderLogger {

    public void logHeader(boolean fkCascadeOn, boolean collectionLoaded, int childSize, int flushInterval) {
        log.info("=== [OneToManyLab] fkCascadeOn={}, collectionLoaded={}, childSize={}, flushInterval={} ===",
                fkCascadeOn, collectionLoaded, childSize, flushInterval);
    }
}
