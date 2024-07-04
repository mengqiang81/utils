这个包主要提供了一些并发开发的辅助工具：
* 自动 copy eagleeye 上下文的线程池 EagleEyeSupportThreadPoolExecutor
* 方便为线程池起名字的 NamedThreadFactory
* 方便并行执行，并可单线程设置超时时间的 FuturesHelper 工具类

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