package cash.ice.ledger.performance;

import cash.ice.common.performance.PerfStopwatch;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public enum TestStrategy {

    SIMPLE {
        @Override
        public PerfStopwatch performInvokes(TestPerformanceRequest request, Runnable logic) {
            logic.run();        // warmup
            log.debug("Starting performance tests...");
            PerfStopwatch logicStopwatch = new PerfStopwatch();
            for (int i = 0; i < request.getTotalInvokes(); i++) {
                logicStopwatch.start();
                logic.run();
                logicStopwatch.stop();
            }
            logicStopwatch.finishStopwatch();
            log.debug("Finished performance tests: " + logicStopwatch);
            return logicStopwatch;
        }
    },

    PARALLEL {
        @Override
        public PerfStopwatch performInvokes(TestPerformanceRequest request, Runnable logic) {
            ExecutorService executorService = Executors.newFixedThreadPool(request.getThreads());
            logic.run();        // warmup
            log.debug("Starting performance tests...");
            PerfStopwatch logicStopwatch = new PerfStopwatch();
            for (int i = 0; i < request.getTotalInvokes(); i++) {
                executorService.submit(() -> {
                    Instant startInstant = Instant.now();
                    logic.run();
                    Instant endInstant = Instant.now();
                    logicStopwatch.addStopwatch(startInstant, endInstant);
                });
            }
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.HOURS)) {
                    throw new IllegalStateException("Tasks run too long!");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Tasks not finished, awaitTermination interrupted!");
            }
            logicStopwatch.finishStopwatch();
            log.debug("Finished performance tests: " + logicStopwatch);
            return logicStopwatch;
        }
    };

    public static TestStrategy of(Integer threads) {
        return threads != null && threads > 1 ? PARALLEL : SIMPLE;
    }

    public abstract PerfStopwatch performInvokes(TestPerformanceRequest request, Runnable logic);
}
