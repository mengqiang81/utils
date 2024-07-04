package com.yintai.serialnumbergen.service

/**
 * Created by Qiang on 02/05/2017.
 */
class HiloAlgorithmDTO {
    String unionCode
    Integer numberLength
    /**
     * start 如果是1000, 则起始值是1001, TODO 如果是-1, 则起始值为一个随机数
     */
    Long start
    Integer maxLo
}
