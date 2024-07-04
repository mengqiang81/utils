package com.alibaba.mos.hsf;

import com.alibaba.mos.base.AbstractSystemException;

import java.util.Optional;

/**
 * 框架级处理系统异常的类，防止接收方反序列化异常
 *
 * @author chigui.meng
 * @date 30/3/2020 11:01 AM
 */
public class CommonSystemException extends AbstractSystemException {
    private static final long serialVersionUID = -2577016454274789403L;

    /**
     * 如果类型是 CommonBusinessException 的话，可以通过 code 字段判断真实类型
     */
    protected String code = "FAIL_SYS_COMMON_SYSTEM_EXCEPTION";

    public String getCode() {
        return code;
    }

    public CommonSystemException(){
        super();
    }

    public CommonSystemException(String code, String messsage, Throwable cause) {
        super(messsage, cause);
        this.code = code;
    }

    public CommonSystemException(String code, String messsage, Throwable cause, Object detail) {
        super(messsage, cause, detail);
        this.code = code;
    }

    @Override
    public String toString() {
        return "CommonSystemException(code=" + Optional.ofNullable(this.getCode()).orElse("") + ", message="
            + Optional.ofNullable(this.getMessage()).orElse("") + ")";
    }
}
