这个包主要提供了一些并发开发的辅助工具：
* 自动 copy eagleeye 上下文的线程池 EagleEyeSupportThreadPoolExecutor
* 方便为线程池起名字的 NamedThreadFactory
* 方便并行执行，并可单线程设置超时时间的 FuturesHelper 工具类


[ ] 还可以提供一个链式异步/同步调用工具方法，返回值可以不同，下一步可以用到上一步的结果作为入参（感觉是不是用标准的StreamAPI就可以实现）
[ ] 在给到的例子里，future没有使用自定义线程池，属于不好的实践
[ ] 可以用结构化并发的思想重构，吧List<Future> 放入scope内部，然后用fork方法add，

```Java
public class MixedTypeStructuredConcurrency {

    // 改进的TaskScope，兼容任意返回类型的Future
    static class TaskScope implements AutoCloseable {
        private final ExecutorService executor = Executors.newCachedThreadPool();
        // 存储所有Future，使用通配符泛型兼容任意类型
        private final List<Future<?>> futures = new ArrayList<>();
        private volatile boolean cancelled = false;

        // 提交任务（支持任意返回类型）
        public <T> Future<T> fork(Supplier<T> task) {
            if (cancelled) {
                throw new IllegalStateException("Scope is cancelled");
            }
            Future<T> future = executor.submit(task::get);
            futures.add(future); // 向上转型为Future<?>，存入列表
            return future;
        }

        // join逻辑：等待所有任务完成，任一失败则取消全部
        public void join() throws InterruptedException, ExecutionException {
            for (Future<?> future : futures) {
                try {
                    // 只需检查任务是否完成，无需关心具体返回值
                    future.get(); 
                } catch (ExecutionException e) {
                    cancelAll(); // 任一任务失败，取消所有任务
                    throw e;
                } catch (CancellationException e) {
                    // 已取消的任务无需处理
                }
            }
        }

        private void cancelAll() {
            cancelled = true;
            for (Future<?> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
            executor.shutdownNow();
        }

        @Override
        public void close() {
            if (!executor.isTerminated()) {
                cancelAll();
            }
        }
    }

    public static void main(String[] args) {
        try (TaskScope scope = new TaskScope()) {
            // 任务A：返回Future<Void>（无返回值）
            Future<Void> logFuture = scope.fork(() -> {
                System.out.println("任务A：记录操作日志...");
                Thread.sleep(500);
                return null; // Void类型需返回null
            });

            // 任务B：返回Future<String>（有返回值）
            Future<String> dataFuture = scope.fork(() -> {
                System.out.println("任务B：获取业务数据...");
                Thread.sleep(1000);
                // 模拟异常：throw new RuntimeException("数据获取失败");
                return "业务数据：12345";
            });

            // 等待所有任务完成（无论返回类型如何）
            scope.join();

            // 分别处理不同类型的结果
            System.out.println("任务A完成状态：" + logFuture.isDone());
            System.out.println("任务B返回数据：" + dataFuture.get());

        } catch (Exception e) {
            System.out.println("任务执行失败：" + e.getMessage());
        }
    }
}
```

举个例子，暂时没时间写单元测试

```Java
@Test
// 测试并发访问 timeout 部分成功的场景
public void testTimeAfter() throws ExecutionException, InterruptedException {
    ScheduledThreadPoolExecutor delayer = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("timeout"),
        new ThreadPoolExecutor.AbortPolicy());

    Map<String, List<Provider>> shopAndProvidersMap = new HashMap<>();
    shopAndProvidersMap.put("1", null);
    shopAndProvidersMap.put("2", null);

    List<CompletableFuture<Pair<String, List<DeliveryStandard>>>> futures = shopAndProvidersMap.entrySet()
        .stream()
        .map(it -> CompletableFuture
            .supplyAsync(() -> this.getAllAvailableDeliveryStandardsReturnShopCode(it.getKey(),
                it.getValue(),
                DeliveryType.APPOINTTIME)
            )
            .applyToEither(FuturesHelper.timeoutAfter(delayer, 100, TimeUnit.MILLISECONDS, "timeout")
                , i -> i)
            .exceptionally((throwable) -> {// 必须在这里把异常处理掉，并且返回 null 值
                String message;
                if (throwable.getCause() != null) {
                    message = throwable.getCause().getMessage();
                } else {
                    message = throwable.getMessage();
                }
                System.out.println(message);
                return null;
            })
        )
        .collect(Collectors.toList());
    CompletableFuture<List<Pair<String, List<DeliveryStandard>>>> allDoneFuture = FuturesHelper.sequence(
        futures);

    Map<String, List<DeliveryStandard>> result =
        allDoneFuture
            .get()
            .stream()
            .filter(Objects::nonNull) // 把 null 值处理掉后就是剩下的部分
            .collect(HashMap::new, (m, v) -> m.put(v.getLeft(), v.getRight()), HashMap::putAll);

    TestCase.assertNotNull(result);
}

private Pair<String, List<DeliveryStandard>> getAllAvailableDeliveryStandardsReturnShopCode(String shopCode,
                                                                                            List<Provider> providers,
                                                                                            DeliveryType deliveryType) {
    try {
        if ("1".equals(shopCode)) {
            Thread.sleep(200);
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return new ImmutablePair<>(shopCode, null);
}
```
