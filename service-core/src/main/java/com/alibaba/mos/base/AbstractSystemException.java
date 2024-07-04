package com.alibaba.mos.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务逻辑异常基类
 * <p>
 * 对于未知的系统异常可以转为具体类型的系统异常抛出，但是通常可以直接由框架级抛出 CommonSystemException
 *
 * @author chigui.meng
 * @date 30/3/2020 11:07 AM
 */
public abstract class AbstractSystemException extends RuntimeException {
    private static final long serialVersionUID = 5902700715961622945L;
    private static String DEFAULT_EXCEPTION_MESSAGE = "未知系统异常";

    protected Object detail;

    public Object getDetail(){
        return detail;
    }

    protected AbstractSystemException() {
        super(DEFAULT_EXCEPTION_MESSAGE);
    }

    protected AbstractSystemException(String message) {
        super(message);
    }

    protected AbstractSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    protected AbstractSystemException(Throwable cause) {
        super(cause);
    }

    protected AbstractSystemException(String message, Object detail) {
        super(message);
        this.detail = detail;
    }

    protected AbstractSystemException(String message, Throwable cause, Object detail) {
        super(message, cause);
        this.detail = detail;
    }

    protected AbstractSystemException(Throwable cause, Object detail) {
        super(cause);
        this.detail = detail;
    }
}
