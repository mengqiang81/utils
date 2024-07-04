package com.alibaba.mos.eagleeye.log;

import com.alibaba.mos.concurrent.EagleEyeSupportThreadPoolExecutor;
import com.taobao.eagleeye.EagleEye;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.entries;

@Slf4j
public class LogbackTest {

    @Test
    public void test1() throws InterruptedException {
        EagleEyeSupportThreadPoolExecutor threadPoolExecutor = new EagleEyeSupportThreadPoolExecutor(10, 10, 30,
            TimeUnit.SECONDS, new SynchronousQueue<>());
        EagleEye.startTrace("111", "sss");
        MDC.put("traceId", EagleEye.getTraceId());
        log.info("222");
        CompletableFuture.runAsync(() -> {
            Map<String, String> myMap = new HashMap<>();
            myMap.put("name1", "value1");
            myMap.put("name2", "value2");
            log.info("xxx {}", entries(myMap));
        }, threadPoolExecutor);
        MDC.remove("traceId");
        Thread.sleep(1000L);
    }

    @Test
    public void test2() {
        SpelExpressionParser spelParser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("systemProperties", System.getProperties());
        context.setVariable("systemEnvironment", System.getenv());

        String systemProperties = spelParser.parseExpression(
            "#systemProperties['sun.cpu.endian']")
            .getValue(context, String.class);

        String systemEnvironment = spelParser.parseExpression(
            "#systemEnvironment['HOME']")
            .getValue(context, String.class);

        boolean condition = spelParser.parseExpression(
            "".equals("1") ? "true" : "#systemProperties['spring.profiles.active']=='staging'||#systemProperties['spring.profiles.active']==null")
            .getValue(context, Boolean.class);

        assert condition;
    }
}
