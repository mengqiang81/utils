package com.alibaba.mos.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by Liutengfei on 2018/2/9
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Sort implements Serializable{
    private static final long serialVersionUID = 237002848607200807L;

    /**
     * 排序字段名称
     */
    private String field;
    /**
     * 正序还是倒序
     */
    private Direction direction;
}
