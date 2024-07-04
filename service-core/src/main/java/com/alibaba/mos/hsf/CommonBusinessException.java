package com.alibaba.mos.hsf;

import com.alibaba.mos.base.AbstractBusinessException;

import java.util.Optional;

/**
 * 框架级处理业务异常的类，防止接收方反序列化异常
 * <p>
 * 这个类不要由业务方使用
 *
 * @author chigui.meng
 * @date 30/3/2020 11:01 AM
 */
public class CommonBusinessException extends AbstractBusinessException {
    private static final long serialVersionUID = 1109677034978059782L;

    /**
     * 如果类型是 CommonBusinessException 的话，可以通过 code 字段判断真实类型
     */
    protected String code = "FAIL_BIZ_COMMON_BUSINESS_EXCEPTION";

    public String getCode() {
        return code;
    }

    public CommonBusinessException(){
        super();
    }

    public CommonBusinessException(String code, String messsage, Throwable cause) {
        super(messsage, cause);
        this.code = code;
    }

    public CommonBusinessException(String code, String messsage, Throwable cause, Object detail) {
        super(messsage, cause, detail);
        this.code = code;
    }

    @Override
    public String toString() {
        return "CommonBusinessException(code=" + Optional.ofNullable(this.getCode()).orElse("") + ", message="
            + Optional.ofNullable(this.getMessage()).orElse("") + ")";
    }
}
