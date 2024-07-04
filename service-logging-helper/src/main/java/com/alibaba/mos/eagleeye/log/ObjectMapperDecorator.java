package com.alibaba.mos.eagleeye.log;

import com.alibaba.mos.base.ObjectMapperFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import net.logstash.logback.decorate.JsonFactoryDecorator;

/**
 * @author chigui.meng
 * @date 9/2/2020 12:49 AM
 */
public class ObjectMapperDecorator implements JsonFactoryDecorator {

    @Override
    public MappingJsonFactory decorate(MappingJsonFactory factory) {
        factory.setCodec(ObjectMapperFactory.getInstance().getObjectMapper());
        return factory;
    }
}
