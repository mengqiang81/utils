package com.alibaba.mos.base.cache;

import java.io.IOException;
import java.io.Serializable;

import com.alibaba.mos.cache.spi.MosSerializeSpi;
import com.alibaba.mos.util.ServiceLocator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * @author huanglitao.hlt
 * @date 2020/04/17
 */
@Slf4j
public class SpecialCacheJacksonSerializeSpi implements MosSerializeSpi {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = ServiceLocator.getBean("cacheObjectMapper",ObjectMapper.class);
    }

    public SpecialCacheJacksonSerializeSpi() {
    }

    @Override
    public Serializable serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException var3) {
            log.error("转换失败！", var3);
            return (Serializable)o;
        }
    }

    @Override
    public Object deserialize(Serializable s) {
        if (!(s instanceof String)) {
            return s;
        } else {
            try {
                return objectMapper.readValue((String)s, Object.class);
            } catch (IOException var3) {
                log.error("数据转换失败！", var3);
                return null;
            }
        }
    }

    @Override
    public String serializeType() {
        return "cachejackson";
    }
}
