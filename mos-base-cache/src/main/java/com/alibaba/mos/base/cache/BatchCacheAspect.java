package com.alibaba.mos.base.cache;

import java.lang.reflect.Method;
import java.util.Collection;

import com.alibaba.mos.cache.AbstractMosCacheManager;
import com.alibaba.mos.cache.AbstractMosCacheManager.TCache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 批量缓存解决方案
 * @author huanglitao.hlt
 * @date 2020/04/15
 */
@ConditionalOnProperty("mos.base.cache.batch.enabled")
@Aspect
@Component
@Slf4j
public class BatchCacheAspect {

    private ExpressionParser parser = new SpelExpressionParser();
    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    @Autowired
    private AbstractMosCacheManager cacheManager;

    @Pointcut("@annotation(com.alibaba.mos.base.cache.BatchCache)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object doArount(ProceedingJoinPoint pjp) throws Throwable {
        Object[] arguments = pjp.getArgs();
        Method method = getMethod(pjp);
        BatchCache batchCache = method.getAnnotation(BatchCache.class);

        if(arguments.length == 0) {
            throw new RuntimeException("使用批量缓存必须要有一个集合参数！");
        }
        int pitemIndex = batchCache.paramItemIndex();
        if(arguments.length <= pitemIndex || !(arguments[pitemIndex] instanceof Collection)) {
            throw new RuntimeException("指定的批量参数不是集合！");
        }

        if(!Collection.class.isAssignableFrom(method.getReturnType())) {
            throw new RuntimeException(("使用批量缓存的方法必须要返回集合数据类型！"));
        }

        Collection batchkeyObjs = (Collection)arguments[pitemIndex];
        // 如果批量查询个数过多，则不走缓存
        if(batchkeyObjs.size() > batchCache.maxCount()) {
            log.warn("由于批量参数[{}]个，超过了最大允许缓存参数[{}]个，将不走缓存！",batchkeyObjs.size(),batchCache.maxCount());
            return pjp.proceed(arguments);
        }
        RootObject root = new RootObject();
        Object target = pjp.getTarget();
        root.setTargetClass(target.getClass());
        root.setTarget(target);
        root.setParams(arguments);
        String pitemExp = batchCache.paramItemExp();
        String pItemName = batchCache.paramItemName();
        String cacheName = batchCache.value();

        Collection nohintkeyObjs = batchCache.paramCollection().newInstance();
        EvaluationContext context = createContext(getMethod(pjp),arguments,pitemExp,root);
        if(!parseSpel(context,batchCache.condition(),Boolean.class)) {
            return pjp.proceed(arguments);
        }
        Collection<Object> results = batchCache.resultCollection().newInstance();
        TCache cache = (TCache)cacheManager.getCache(cacheName);
        if(cache == null) {
            throw new RuntimeException("缓存["+ cacheName + "]没有进行定义！");
        }
        batchkeyObjs.stream().forEach( batchKeyObj -> {
            if(batchKeyObj == null) {
                return;
            }
            context.setVariable(pItemName,batchKeyObj);
            String pkey = parseSpel(context,pitemExp,String.class);

            ValueWrapper vw = cache.get(pkey);
            if(vw != null) {
                Object result = vw.get();
                results.add(result);
                return;
            }
            nohintkeyObjs.add(batchKeyObj);
        });
        if(nohintkeyObjs.isEmpty()) {
            return results;
        }
        arguments[pitemIndex] = nohintkeyObjs;
        Collection<Object> returnObjs = (Collection<Object>)pjp.proceed(arguments);
        if(returnObjs == null || returnObjs.isEmpty()) {
            return results;
        }
        String ritemExp = batchCache.resultItemExp();
        String ritemName = batchCache.resultItemName();
        String unlessExp = batchCache.unless();
        returnObjs.forEach(returnObj -> {
            if(returnObj == null) {
                return;
            }
            context.setVariable(ritemName,returnObj);
            String rkey = parseSpel(context,ritemExp,String.class);
            boolean unless = false;
            if(StringUtils.isNotBlank(unlessExp)) {
                unless = parseSpel(context,unlessExp,Boolean.class);
            }
            if(!unless) {
                cache.put(rkey,returnObj);
            }
            results.add(returnObj);
        });
        return results;
    }

    /**
     * 获取被切面拦截的方法
     * @param joinPoint
     * @return
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint
                    .getTarget()
                    .getClass()
                    .getDeclaredMethod(joinPoint.getSignature().getName(),
                        method.getParameterTypes());
            } catch (SecurityException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return method;
    }

    /**
     * 解析spel表达式
     * @param context
     * @param spel
     * @param cls
     * @param <T>
     * @return
     */
    private <T> T parseSpel(EvaluationContext context,String spel,Class<T> cls) {
        try {
            if(!spel.startsWith("#{") && !spel.endsWith("}")) {
                spel = "#{" + spel + "}";
            }
            Expression expression = parser.parseExpression(spel,new TemplateParserContext());
            return expression.getValue(context,cls);
        } catch (Exception e) {
            throw new RuntimeException("解析Spel表达式错误！",e);
        }
    }

    /**
     * 设置表达式值
     * @param method
     * @param arguments
     * @param spel
     * @param root
     * @return
     */
    private EvaluationContext createContext(Method method, Object[] arguments, String spel,RootObject root) {
        String[] params = discoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        ((StandardEvaluationContext)context).setRootObject(root);
        //context.setVariable("root",root);
        for (int len = 0; len < params.length; len++) {
            context.setVariable("p" + len,arguments[len]);
            context.setVariable(params[len], arguments[len]);
        }
        return context;
    }

    /**
     * 结构对象
     */
    @Data
    public static class RootObject {

        private Class<?> targetClass;

        private Object target;

        private Object[] params;

        private Object result;
    }
}
