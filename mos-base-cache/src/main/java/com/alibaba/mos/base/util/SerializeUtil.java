package com.alibaba.mos.base.util;

import java.io.Serializable;
import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

/**
 * @author huanglitao.hlt
 * @date 2020/04/24
 */
@Slf4j
public class SerializeUtil {

    /**
     * 将源对象拷贝为可序列化的序列化对象
     * @param source
     * @param targetClass
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S,T extends Serializable> T toSerialize(S source,Class<T> targetClass) {
        if(source == null) {
            return null;
        }
        return BeanUtils.copyBean(source,targetClass);
    }

    /**
     * 将集合对象拷贝为可序列化的集合序列化对象
     * @param sources
     * @param targetClass
     * @param collectionClass
     * @param <S>
     * @param <T>
     * @param <C>
     * @return
     */
    public static <S extends Collection,T extends Serializable,C extends  Collection> C toSerialize(S sources,Class<T> targetClass,Class<C> collectionClass) {
        if(sources == null || sources.isEmpty()) {
            return null;
        }
        Collection<T> results = null;
        try {
            results = collectionClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("实例化集合对象时发生错误！[{}]",collectionClass,e);
            throw new RuntimeException("实例化集合对象时发生错误！");
        }
        for(Object source : sources) {
            results.add(toSerialize(source,targetClass));
        }
        return (C)results;
    }
}
