package com.alibaba.mos.serialnumbergen.impl;

public class SerialNumberException extends RuntimeException {
    public SerialNumberException(String msg) {
        super(msg);
    }

    public SerialNumberException(Throwable throwable) {
        super(throwable);
    }
}
