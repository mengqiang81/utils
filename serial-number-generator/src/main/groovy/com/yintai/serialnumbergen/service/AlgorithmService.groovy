package com.yintai.serialnumbergen.service

/**
 * Created by Qiang on 27/04/2017.
 */
interface AlgorithmService<T> {
    String gen(T algorithmParams)
    void init(T algorithmParams)
}