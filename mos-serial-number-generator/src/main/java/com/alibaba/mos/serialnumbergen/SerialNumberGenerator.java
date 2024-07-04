package com.alibaba.mos.serialnumbergen;

public interface SerialNumberGenerator {
    /**
     * 获取流水号
     * @return
     */
    String next();
}
