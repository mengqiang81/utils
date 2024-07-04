package com.alibaba.mos.base.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 批量缓存
 * @author huanglitao.hlt
 * @date 2020/04/15
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BatchCache {

    /**
     * 集合参数表达式名称
     * @return
     */
    String paramItemName() default "pitem";

    /**
     * 集合参数在参数中的索引位置
     * @return
     */
    int paramItemIndex() default 0;

    /**
     * 单个缓存key表达式
     * @return
     */
    String paramItemExp() default "#pitem";

    /**
     * 集合参数具体集合实现类
     * @return
     */
    Class<? extends Collection> paramCollection() default ArrayList.class;

    /**
     * 缓存名称
     * @return
     */
    String value();

    /**
     * 返回值表达式名称
     * @return
     */
    String resultItemName() default "ritem";

    /**
     * 返回值单个对象写入缓存key表达式
     * @return
     */
    String resultItemExp() default "#ritem";

    /**
     * 是否启用
     * @return
     */
    String condition() default "true";

    /**
     * 要排除的（不使用缓存）
     * @return
     */
    String unless() default "";

    /**
     * 返回值集合具体实现类
     * @return
     */
    Class<? extends Collection> resultCollection() default ArrayList.class;

    /**
     * 采用批量缓存最大个数，如果超过则不走缓存
     * @return
     */
    int maxCount() default 20;
}
