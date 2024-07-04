package com.alibaba.mos.eagleeye.log;

import com.taobao.eagleeye.EagleEye;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
/**
 * 虽然用到了 logstash-logback-encoder 的能力, 但是这个方法本身返回的是个 Marker, 所以理论上在切换非 logback 后端时可以做到一定程度的兼容
 */
import static net.logstash.logback.marker.Markers.appendEntries;

/**
 * 需要添加监控的日志切面
 *
 * @author chigui.mq
 * @date 14/04/2017
 */
@Aspect
@Slf4j
public class ServiceLoggingAspect {

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

    private SpelExpressionParser spelParser = new SpelExpressionParser();

    @Pointcut(
        "@within(com.alibaba.mos.eagleeye.log.ServiceMonitor) || @annotation(com.alibaba.mos.eagleeye.log"
            + ".ServiceMonitor)")
    public void pointcut() {}

    /**
     * logger
     * @param joinPoint
     * @return Object
     * @throws Throwable
     */
    @Around("pointcut()")
    public Object executionServiceLogger(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        // XXX 这个地方应该利用 eagleeye 自带的 mdc 机制 spring.eagleeye.mdc-updater=slf4j，现在这么写只是为了兼容老的日志规范
        String traceId = EagleEye.getTraceId();
        String rpcId = EagleEye.getRpcId();

        ServiceMonitor serviceMonitor;
        InvokeType invokeType;
        String invokeTypeValue = null;
        Class<? extends Throwable>[] bizClasses = null;

        final Object[] args = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
        final String signature = methodSignature.toLongString();

        // XXX sunfire 监控不支持复杂的 method 解析（主要是参数间的逗号问题），做个简化版本
        final String sunfireMethod = String.join(SPLITE, methodSignature.getMethod().getDeclaringClass().getName(),methodSignature.getName());

        String[] paramNameList = methodSignature.getParameterNames();
        boolean condition = true;
        boolean isTesting = ContextUtil.isTesting();
        try {
            Class target = joinPoint.getTarget().getClass();
            Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();

            if (method.isAnnotationPresent(ServiceMonitor.class)) {
                serviceMonitor = method.getAnnotation(ServiceMonitor.class);
            } else if (target.isAnnotationPresent(ServiceMonitor.class)) {
                serviceMonitor = (ServiceMonitor)target.getAnnotation(ServiceMonitor.class);
            } else {
                serviceMonitor = null;
            }

            if (serviceMonitor != null) {
                invokeType = serviceMonitor.invokeType();
                bizClasses = serviceMonitor.bizClasses();
                invokeTypeValue = invokeType.getValue();

                // 解析SpEL表达式获取结果

                //将方法的参数名和参数值一一对应的放入上下文中
                EvaluationContext context = new StandardEvaluationContext(joinPoint.getTarget());
                for (int i = 0; i < paramNameList.length; i++) {
                    context.setVariable(paramNameList[i], args[i]);
                }
                context.setVariable("systemProperties", System.getProperties());
                context.setVariable("systemEnvironment", System.getenv());

                condition = spelParser.parseExpression(
                    "".equals(serviceMonitor.condition()) ? "true" : serviceMonitor.condition())
                    .getValue(context, Boolean.class);
            }

        } catch (Exception e) {
            log.warn("日志切面中间件错误", e);
        }

        Object result = null;
        Throwable exception = null;

        if (log.isDebugEnabled() && condition) {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put(INVOKE_TYPE, invokeTypeValue);
            requestMap.put(INVOKE_STAGE, InvokeStage.REQUEST.getValue());
            requestMap.put(INVOKE_METHOD, signature);
            requestMap.put(SUNFIRE_METHOD, sunfireMethod);
            requestMap.put(INVOKE_PARAMS, args);
            requestMap.put(FIELD_RPC_ID, rpcId);
            requestMap.put(FIELD_TRACE_ID, traceId);
            requestMap.put(INVOKE_EXCEPTION, EMPTY);
            requestMap.put(INVOKE_TEST, isTesting);
            log.debug(appendEntries(requestMap), InvokeStage.REQUEST.getDesc());
        }

        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (log.isWarnEnabled() && condition) {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put(INVOKE_TYPE, invokeTypeValue);
                errorMap.put(INVOKE_STAGE, InvokeStage.THROW.getValue());
                errorMap.put(INVOKE_METHOD, signature);
                errorMap.put(SUNFIRE_METHOD, sunfireMethod);
                errorMap.put(INVOKE_PARAMS, args);
                errorMap.put(INVOKE_DURATION, duration);
                errorMap.put(FIELD_RPC_ID, rpcId);
                errorMap.put(FIELD_TRACE_ID, traceId);
                errorMap.put(INVOKE_EXCEPTION, throwable.getClass().getName());
                errorMap.put(INVOKE_TEST, isTesting);

                if (isBizException(throwable, bizClasses)) {
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

            if (log.isDebugEnabled() && exception == null && condition) {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put(INVOKE_TYPE, invokeTypeValue);
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
}