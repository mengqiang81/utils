package com.alibaba.mos.eagleeye.log;

import com.alibaba.mos.base.AbstractBusinessException;
import com.taobao.eagleeye.EagleEye;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static net.logstash.logback.marker.Markers.appendEntries;

/**
 * @author chigui.meng
 * @date 9/5/2020 6:42 PM
 */
@Slf4j
public class ServiceLoggingInterceptor implements MethodInterceptor {

    private static final String SPLITE = "#";
    private static final String EMPTY = "";

    private static final String FIELD_TRACE_ID = "trace_id";
    private static final String FIELD_RPC_ID = "rpc_id";

    /**
     * 服务调用类型, 包含 rpc, topic, external，目前只有 rpc，剩下的两种并没有被实现
     */
    private static final String INVOKE_TYPE = "type";
    /**
     * 服务调用阶段
     */
    private static final String INVOKE_STAGE = "stage";
    /**
     * 服务调用方法
     */
    private static final String INVOKE_METHOD = "method";
    /**
     * sunfire监控使用的方法字段 service#method
     */
    private static final String SUNFIRE_METHOD = "sunfire_method";
    /**
     * 服务调用参数
     */
    private static final String INVOKE_PARAMS = "params";
    /**
     * 服务调用结果
     */
    private static final String INVOKE_RESULT = "result";
    /**
     * 服务调用时长
     */
    private static final String INVOKE_DURATION = "duration";
    /**
     * 服务调用是否成功, 业务异常算成功
     */
    private static final String INVOKE_SUCCESS = "success";
    /**
     * 服务调用异常类型, 包含 biz_error 和 sys_error
     */
    private static final String INVOKE_ERROR_TYPE = "error_type";
    /**
     * 异常对象，sunfire监控指定异常可用
     */
    private static final String INVOKE_EXCEPTION = "exception";
    /**
     * 是否测试调用
     */
    private static final String INVOKE_TEST = "test";

    private InvokeType invokeType;

    private Class<? extends Throwable>[] bizClasses;

    public ServiceLoggingInterceptor() {
        this.invokeType = InvokeType.CONSUMER;
        this.bizClasses = new Class[] {AbstractBusinessException.class, IllegalArgumentException.class};
    }

    public ServiceLoggingInterceptor(InvokeType invokeType) {
        assert invokeType != null;
        this.invokeType = invokeType;
        this.bizClasses = new Class[] {AbstractBusinessException.class, IllegalArgumentException.class};
    }

    public ServiceLoggingInterceptor(InvokeType invokeType, Class<? extends Throwable>[] bizClasses) {
        assert invokeType != null;
        assert bizClasses != null;
        this.invokeType = invokeType;
        this.bizClasses = bizClasses;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        long startTime = System.currentTimeMillis();

        // XXX 这个地方应该利用 eagleeye 自带的 mdc 机制 spring.eagleeye.mdc-updater=slf4j，现在这么写只是为了兼容老的日志规范
        String traceId = EagleEye.getTraceId();
        String rpcId = EagleEye.getRpcId();

        final Object[] args = methodInvocation.getArguments();
        MethodSignatureImpl methodSignature = new MethodSignatureImpl(methodInvocation);
        final String signature = methodSignature.toLongString();

        // XXX sunfire 监控不支持复杂的 method 解析（主要是参数间的逗号问题），做个简化版本
        final String sunfireMethod = String.join(SPLITE, methodSignature.getDeclaringType().getName(),methodSignature.getName());

        Object result = null;
        Throwable exception = null;
        
        boolean isTesting = ContextUtil.isTesting();

        if (log.isDebugEnabled()) {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put(INVOKE_TYPE, invokeType.getValue());
            requestMap.put(INVOKE_STAGE, InvokeStage.REQUEST.getValue());
            requestMap.put(INVOKE_METHOD, signature);
            requestMap.put(SUNFIRE_METHOD, sunfireMethod);
            requestMap.put(INVOKE_PARAMS, args);
            requestMap.put(FIELD_RPC_ID, rpcId);
            requestMap.put(FIELD_TRACE_ID, traceId);
            requestMap.put(INVOKE_EXCEPTION, EMPTY);
            requestMap.put(INVOKE_TEST, isTesting);
            //XXX 日志会在 message 里和json 里被打印两次，这个数据量值得考虑
            log.debug(appendEntries(requestMap), InvokeStage.REQUEST.getDesc());
        }

        try {
            result = methodInvocation.proceed();
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (log.isWarnEnabled()) {
                Map<String, Object> errorMap = new HashMap<>();

                errorMap.put(INVOKE_TYPE, invokeType.getValue());
                errorMap.put(INVOKE_STAGE, InvokeStage.THROW.getValue());
                errorMap.put(INVOKE_METHOD, signature);
                errorMap.put(SUNFIRE_METHOD, sunfireMethod);
                errorMap.put(INVOKE_PARAMS, args);
                errorMap.put(INVOKE_DURATION, duration);
                errorMap.put(FIELD_RPC_ID, rpcId);
                errorMap.put(FIELD_TRACE_ID, traceId);
                errorMap.put(INVOKE_EXCEPTION, throwable.toString());
                errorMap.put(INVOKE_TEST, isTesting);

                if (isBizException(throwable, this.bizClasses)) {
                    errorMap.put(INVOKE_ERROR_TYPE, InvokeErrorType.BIZ.getValue());
                    errorMap.put(INVOKE_SUCCESS, true);
                    log.warn(appendEntries(errorMap), InvokeErrorType.BIZ.getDesc(), throwable);
                } else {
                    errorMap.put(INVOKE_ERROR_TYPE, InvokeErrorType.SYS.getValue());
                    errorMap.put(INVOKE_SUCCESS, false);
                    log.error(appendEntries(errorMap), InvokeErrorType.SYS.getDesc(), throwable);
                }
            }
            exception = throwable;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (log.isDebugEnabled() && exception == null) {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put(INVOKE_TYPE, invokeType.getValue());
                responseMap.put(INVOKE_STAGE, InvokeStage.RESPONSE.getValue());
                responseMap.put(INVOKE_METHOD, signature);
                responseMap.put(SUNFIRE_METHOD, sunfireMethod);
                responseMap.put(INVOKE_DURATION, duration);
                responseMap.put(INVOKE_PARAMS, args);
                responseMap.put(INVOKE_RESULT, result);
                responseMap.put(INVOKE_SUCCESS, true);
                responseMap.put(FIELD_RPC_ID, rpcId);
                responseMap.put(FIELD_TRACE_ID, traceId);
                responseMap.put(INVOKE_EXCEPTION, EMPTY);
                responseMap.put(INVOKE_TEST, isTesting);

                //XXX 仔细考量日志级别问题, 假设生产不会打印 debug, 那么我们还要保留什么? 我觉得可以不要 result 以减少开销, param 有待考虑
                log.debug(appendEntries(responseMap), InvokeStage.RESPONSE.getDesc());
            }

            if (exception != null) {
                throw exception;
            }
        }

        return result;
    }

    private boolean isBizException(Throwable ex, Class<? extends Throwable>[] bizClasses) {
        if (bizClasses == null || bizClasses.length == 0) {
            return false;
        }
        if (ex instanceof ExecutionException && ex.getCause() != null) {
            ex = ex.getCause();
        }
        for (Class<? extends Throwable> it : bizClasses) {
            if (it.isInstance(ex)) {
                return true;
            }
        }
        return false;
    }

    private class MethodSignatureImpl {
        private MethodInvocation methodInvocation;

        private MethodSignatureImpl(MethodInvocation methodInvocation) {
            this.methodInvocation = methodInvocation;
        }

        public String getName() {
            return methodInvocation.getMethod().getName();
        }

        public int getModifiers() {
            return methodInvocation.getMethod().getModifiers();
        }

        public Class<?> getDeclaringType() {
            return methodInvocation.getMethod().getDeclaringClass();
        }

        public String getDeclaringTypeName() {
            return methodInvocation.getMethod().getDeclaringClass().getName();
        }

        public Class<?> getReturnType() {
            return methodInvocation.getMethod().getReturnType();
        }

        public Method getMethod() {
            return methodInvocation.getMethod();
        }

        public Class<?>[] getParameterTypes() {
            return methodInvocation.getMethod().getParameterTypes();
        }

        public String toShortString() {
            return this.toString(false, false, false, false);
        }

        public String toLongString() {
            return this.toString(true, true, true, true);
        }

        @Override
        public String toString() {
            return this.toString(false, true, false, true);
        }

        private String toString(boolean includeModifier, boolean includeReturnTypeAndArgs,
                                boolean useLongReturnAndArgumentTypeName, boolean useLongTypeName) {
            StringBuilder sb = new StringBuilder();
            if (includeModifier) {
                sb.append(Modifier.toString(this.getModifiers()));
                sb.append(" ");
            }

            if (includeReturnTypeAndArgs) {
                this.appendType(sb, this.getReturnType(), useLongReturnAndArgumentTypeName);
                sb.append(" ");
            }

            this.appendType(sb, this.getDeclaringType(), useLongTypeName);
            sb.append(".");
            sb.append(this.getMethod().getName());
            sb.append("(");
            Class<?>[] parametersTypes = this.getParameterTypes();
            this.appendTypes(sb, parametersTypes, includeReturnTypeAndArgs, useLongReturnAndArgumentTypeName);
            sb.append(")");
            return sb.toString();
        }

        private void appendTypes(StringBuilder sb, Class<?>[] types, boolean includeArgs,
                                 boolean useLongReturnAndArgumentTypeName) {
            if (includeArgs) {
                int size = types.length;

                for (int i = 0; i < size; ++i) {
                    this.appendType(sb, types[i], useLongReturnAndArgumentTypeName);
                    if (i < size - 1) {
                        sb.append(",");
                    }
                }
            } else if (types.length != 0) {
                sb.append("..");
            }

        }

        private void appendType(StringBuilder sb, Class<?> type, boolean useLongTypeName) {
            if (type.isArray()) {
                this.appendType(sb, type.getComponentType(), useLongTypeName);
                sb.append("[]");
            } else {
                sb.append(useLongTypeName ? type.getName() : type.getSimpleName());
            }

        }
    }
}
