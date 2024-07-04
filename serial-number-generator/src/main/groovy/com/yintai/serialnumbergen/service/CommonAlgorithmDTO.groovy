package com.yintai.serialnumbergen.service

/**
 * Created by Qiang on 05/05/2017.
 */
class CommonAlgorithmDTO {
    String unionCode
    Integer numberLength
    /**
     * 开始数, TODO 支持随机数开始
     */
    Long start
    Integer maxLo

    /**
     * 日期格式化 patten, 默认为 yyyyMMdd
     */
    String dateFormat
    /**
     * 时区的字符串表示, 比如中国的就是 Asia/Shanghai, 不填的话默认使用UTC
     */
    String zone

    /**
     * 前缀
     */
    String prefix
    /**
     * 后缀
     */
    String postfix
}
