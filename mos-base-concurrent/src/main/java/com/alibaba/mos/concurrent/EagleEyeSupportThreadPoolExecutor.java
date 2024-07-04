package com.alibaba.mos.concurrent;

import com.taobao.eagleeye.EagleEye;
import org.slf4j.MDC;

import java.util.concurrent.*;

public class EagleEyeSupportThreadPoolExecutor extends ThreadPoolExecutor {

    // XXX 这个地方应该利用 eagleeye 自带的 mdc 机制 spring.eagleeye.mdc-updater=slf4j，现在这么写只是为了兼容老的日志规范
    private static final String FIELD_TRACE_ID = "trace_id";
    private static final String FIELD_RPC_ID = "rpc_id";

    /**
     * The default rejected execution handler
     */
    private static final RejectedExecutionHandler defaultHandler =
        new AbortPolicy();

    public EagleEyeSupportThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public EagleEyeSupportThreadPoolExecutor(int corePoolSize,
                                             int maximumPoolSize,
                                             long keepAliveTime,
                                             TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue,
                                             RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            Executors.defaultThreadFactory(), handler);
    }

    public EagleEyeSupportThreadPoolExecutor(int corePoolSize,
                                             int maximumPoolSize,
                                             long keepAliveTime,
                                             TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue,
                                             ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            threadFactory, defaultHandler);
    }

    public EagleEyeSupportThreadPoolExecutor(int corePoolSize,
                                             int maximumPoolSize,
                                             long keepAliveTime,
                                             TimeUnit unit,
                                             BlockingQueue<Runnable> workQueue,
                                             ThreadFactory threadFactory,
                                             RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value) {
            final Object rpcContext = EagleEye.currentRpcContext();

            public void run() {
                try {
                    EagleEye.setRpcContext(rpcContext);
                    String traceId = EagleEye.getTraceId();
                    String rpcId = EagleEye.getRpcId();
                    if (traceId != null && !"".equals(traceId)) {
                        MDC.put(FIELD_TRACE_ID, traceId);
                    }
                    if (rpcId != null && !"".equals(rpcId)) {
                        MDC.put(FIELD_RPC_ID, rpcId);
                    }
                    super.run();
                } finally {
                    EagleEye.clearRpcContext();
                    MDC.remove(FIELD_TRACE_ID);
                    MDC.remove(FIELD_RPC_ID);
                }
            }
        };
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable) {
            final Object rpcContext = EagleEye.currentRpcContext();

            public void run() {
                try {
                    EagleEye.setRpcContext(rpcContext);
                    String traceId = EagleEye.getTraceId();
                    String rpcId = EagleEye.getRpcId();
                    if (traceId != null && !"".equals(traceId)) {
                        MDC.put(FIELD_TRACE_ID, traceId);
                    }
                    if (rpcId != null && !"".equals(rpcId)) {
                        MDC.put(FIELD_RPC_ID, rpcId);
                    }
                    super.run();
                } finally {
                    EagleEye.clearRpcContext();
                    MDC.remove(FIELD_TRACE_ID);
                    MDC.remove(FIELD_RPC_ID);
                }
            }
        };
    }

    class EagleEyeSupportRunnableWrapper implements Runnable {
        private final Runnable runnable;
        private final Object rpcContext;

        public EagleEyeSupportRunnableWrapper(Runnable runnable, Object rpcContext) {
            this.runnable = runnable;
            this.rpcContext = rpcContext;
        }

        @Override
        public void run() {
            try {
                EagleEye.setRpcContext(rpcContext);
                String traceId = EagleEye.getTraceId();
                String rpcId = EagleEye.getRpcId();
                if (traceId != null && !"".equals(traceId)) {
                    MDC.put(FIELD_TRACE_ID, traceId);
                }
                if (rpcId != null && !"".equals(rpcId)) {
                    MDC.put(FIELD_RPC_ID, rpcId);
                }
                runnable.run();
            } finally {
                EagleEye.clearRpcContext();
                MDC.remove(FIELD_TRACE_ID);
                MDC.remove(FIELD_RPC_ID);
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        super.execute(new EagleEyeSupportRunnableWrapper(command, EagleEye.currentRpcContext()));
    }
}
