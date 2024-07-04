package com.alibaba.mos.eagleeye.log;

/**
 * 所有的调用类型都可以抽象为 consumer（RPC 和 消息的 consumer） 和 producer（对外调用，消息的 producer，对数据库的请求等）
 * <p>
 * 所有的 producer 在单元测试中都应该被 mock
 *
 * @author chigui.meng
 * @date 28/4/2021 10:21 AM
 */
public enum InvokeType {
    /**
     * 服务调用类型 consumer
     */
    CONSUMER("consumer"),

    /**
     * 服务调用类型 producer
     */
    PRODUCER("producer");

    private String value;

    private InvokeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
