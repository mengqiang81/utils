package com.alibaba.mos.eagleeye.log;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.fasterxml.jackson.core.JsonGenerator;
import com.taobao.eagleeye.EagleEye;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import java.io.IOException;

/**
 * @param <Event>
 * @deprecated traceId 有两个地方设置到 MDC 里，一个是日志切面，一个是支持 eagleEye 的线程池，所以这地方没用了
 */
@Deprecated
public class EagleEyeProvider<Event extends DeferredProcessingAware> extends AbstractFieldJsonProvider<Event>
    implements FieldNamesAware<LogstashFieldNames> {
    private static final String FIELD_TRACE_ID = "trace_id";
    private static final String FIELD_RPC_ID = "rpc_id";
    private static final String FIELD_APP_NAME = "app_name";
    private static final String PROJECT_NAME = "project.name";

    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
        setFieldName(FIELD_TRACE_ID);
        setFieldName(FIELD_APP_NAME);
    }

    @Override
    public void writeTo(JsonGenerator jsonGenerator, Event event) throws IOException {

        String traceId = EagleEye.getTraceId();
        String rpcId = EagleEye.getRpcId();

        String appName = null;
        try {
            appName = System.getProperty(PROJECT_NAME);
        } catch (Throwable t) {
            // 啥也不干
        }

        if (traceId != null && !"".equals(traceId)) {
            JsonWritingUtils.writeStringField(jsonGenerator, FIELD_TRACE_ID, traceId);
        }
        if (rpcId != null && !"".equals(rpcId)) {
            JsonWritingUtils.writeStringField(jsonGenerator, FIELD_RPC_ID, rpcId);
        }
        if (appName != null && !"".equals(appName)) {
            JsonWritingUtils.writeStringField(jsonGenerator, FIELD_APP_NAME, appName);
        }
    }
}
