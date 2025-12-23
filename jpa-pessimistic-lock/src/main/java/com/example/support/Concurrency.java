package com.example.support;


import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class Concurrency {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private Concurrency() {}

    public static List<Throwable> runConcurrently(
            List<? extends Callable<Void>> tasks
    ) throws InterruptedException {
        return runConcurrently(tasks.size(), tasks, DEFAULT_TIMEOUT);
    }

    public static List<Throwable> runConcurrently(
            int threads,
            List<? extends Callable<Void>> tasks
    ) throws InterruptedException {
        return runConcurrently(threads, tasks, DEFAULT_TIMEOUT);
    }

    public static List<Throwable> runConcurrently(
            int threads,
            List<? extends Callable<Void>> tasks,
            Duration timeout
    ) throws InterruptedException {

        if (threads <= 0) throw new IllegalArgumentException("threads must be positive");
        if (tasks == null || tasks.isEmpty()) throw new IllegalArgumentException("tasks must not be null/empty");
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = pool.invokeAll(tasks, timeout.toMillis(), TimeUnit.MILLISECONDS);

            List<Throwable> errors = new ArrayList<>();
            for (Future<Void> f : futures) {
                try {
                    f.get();
                } catch (CancellationException e) {
                    errors.add(new TimeoutException("Task timed out and was cancelled"));
                } catch (ExecutionException e) {
                    errors.add(e.getCause());
                }
            }
            return errors;
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
