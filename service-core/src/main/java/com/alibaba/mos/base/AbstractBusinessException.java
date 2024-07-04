package com.alibaba.mos.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 业务逻辑异常基类
 * <p>
 * 所有业务系统能够捕获的异常都要转换为具体类型的业务逻辑异常抛出，如OrderNotFoundException
 *
 * @author chigui.meng
 */
public abstract class AbstractBusinessException extends RuntimeException {

    private static final long serialVersionUID = 3734398223565266701L;
    private static String DEFAULT_EXCEPTION_MESSAGE = "业务逻辑异常";

    protected Object detail;

    public Object getDetail(){
        return detail;
    }

    protected AbstractBusinessException() {
        super(DEFAULT_EXCEPTION_MESSAGE);
    }

    protected AbstractBusinessException(String message) {
        super(message);
    }

    protected AbstractBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    protected AbstractBusinessException(Throwable cause) {
        super(cause);
    }

    protected AbstractBusinessException(String message, Object detail) {
        super(message);
        this.detail = detail;
    }

    protected AbstractBusinessException(String message, Throwable cause, Object detail) {
        super(message, cause);
        this.detail = detail;
    }

    protected AbstractBusinessException(Throwable cause, Object detail) {
        super(cause);
        this.detail = detail;
    }
}
