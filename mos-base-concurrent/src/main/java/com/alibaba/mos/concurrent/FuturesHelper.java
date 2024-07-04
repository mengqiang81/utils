package com.alibaba.mos.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * @author chigui.meng
 * @date 6/2/2020 12:50 PM
 */
public class FuturesHelper {
    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(
            v -> futures.stream().map(CompletableFuture::join).collect(Collectors.<T>toList()));
    }

    public static <T> CompletableFuture<T> timeoutAfter(ScheduledThreadPoolExecutor delayer, long timeout,
                                                        TimeUnit unit, String message) {
        CompletableFuture<T> result = new CompletableFuture<>();
        delayer.schedule(() -> result.completeExceptionally(new TimeoutException(message)), timeout, unit);
        return result;
    }
}
