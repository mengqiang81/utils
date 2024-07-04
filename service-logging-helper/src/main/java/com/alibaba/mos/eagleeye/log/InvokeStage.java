package com.alibaba.mos.eagleeye.log;

/**
 * @author chigui.meng
 * @date 28/4/2021 10:16 AM
 */
public enum InvokeStage {
    /**
     * 在服务被调用时, 搜 type:request
     */
    REQUEST("request", "service request"),
    /**
     * 在服务正常返回时, 搜 type:response，这个时候 success 为 true
     */
    RESPONSE("response", "service response"),
    /**
     * 在服务异常返回时, 搜 type:throw, 这个 throw 包含业务异常和系统异常, 可以用 error_type 判断, biz 为业务异常, sys 为系统异常。业务异常 success 为 true，系统异常
     * success 为 false
     */
    THROW("throw", "service throw");

    private String value;

    private String desc;

    private InvokeStage(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
