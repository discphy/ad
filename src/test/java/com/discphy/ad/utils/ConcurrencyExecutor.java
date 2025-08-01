package com.discphy.ad.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class ConcurrencyExecutor {

    public static void executeConcurrency(int threadCount, Runnable runnable) {
        executeConcurrency(IntStream.range(0, threadCount)
            .mapToObj(i -> runnable)
            .toList());
    }

    public static void executeConcurrency(List<Runnable> runnables) {
        ExecutorService executorService = Executors.newFixedThreadPool(runnables.size());

        List<CompletableFuture<Void>> futures = runnables.stream()
            .map(runnable -> CompletableFuture.runAsync(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, executorService))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
    }
}
