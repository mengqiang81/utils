package com.alibaba.mos.eagleeye.log;

/**
 * @author chigui.meng
 * @date 28/4/2021 10:17 AM
 */
public enum InvokeErrorType {
    /**
     * 业务异常
     */
    BIZ("biz_error","service biz error"),
    /**
     * 系统异常
     */
    SYS("sys_error","service sys error");

    private String value;

    private String desc;

    private InvokeErrorType(String value, String desc) {
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
