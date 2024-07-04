package com.alibaba.mos.eagleeye.log;

import com.alibaba.mos.base.AbstractBusinessException;

import java.lang.annotation.*;

/**
 * @author chigui.meng
 * @date 9/2/2020 1:35 AM
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceMonitor {
    /**
     * 该监控的调用类型
     * <p>
     * 通常如果你是服务提供者的话，类型是 InvokeType.CONSUMER；如果你要切的是一个三方依赖，那这个方法类型可以是 InvokeType.PRODUCER，在单测里，这样的方法应该被 Mock
     *
     * @return InvokeType
     */
    InvokeType invokeType() default InvokeType.CONSUMER;

    /**
     * 是否打印日志，支持 spel 表达式，XXX 目前不支持@bean，简单支持systemProperties，systemEnvironment 作为变量，不知道有什么坑
     * <p>
     * 比如对于对三方的调用场景，希望生产环境不打印日志的话，可以这么写 @ServiceMonitor(invokeType = InvokeType.PRODUCER, condition =
     * "#systemProperties['spring.profiles.active']!='production'")
     *
     * @return condition value
     */
    String condition() default "";

    /**
     * 业务异常基类
     * <p>
     * 默认包含AbstractBusinessException，IllegalArgumentException 如果要自己修改的话，别忘了把这些加上
     *
     * @return throwable class
     */
    Class<? extends Throwable>[] bizClasses() default {AbstractBusinessException.class, IllegalArgumentException.class};
}
