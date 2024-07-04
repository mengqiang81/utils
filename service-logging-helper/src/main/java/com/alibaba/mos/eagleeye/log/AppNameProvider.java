package com.alibaba.mos.eagleeye.log;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;
import com.taobao.eagleeye.EagleEye;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import java.io.IOException;

public class AppNameProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event>
    implements FieldNamesAware<LogstashFieldNames> {
    private static final String FIELD_APP_NAME = "app_name";
    private static final String PROJECT_NAME = "project.name";

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(FIELD_APP_NAME);
    }

    @Override
    public void writeTo(JsonGenerator jsonGenerator, Event event) throws IOException {
        String appName = null;
        try {
            appName = System.getProperty(PROJECT_NAME);
        } catch (Throwable t) {
            // 啥也不干
        }

        if (appName != null && !"".equals(appName)) {
            JsonWritingUtils.writeStringField(jsonGenerator, FIELD_APP_NAME, appName);
        }
    }
}
