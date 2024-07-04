package com.alibaba.mos.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 支持滚动查询的结果集，内含总条目数，没页信息条数, 本次查询的最后一条记录的定位值等信息
 *
 * @author Richard  Zhang
 */
@Setter
@Getter
public class SearchAfterPagedList<T> implements Serializable{
    private static final long serialVersionUID = -4371622131108133262L;
    /**
     * 信息总数
     */
    private long totalCount;
    /**
     * 每页的信息条数
     */
    private int pageSize;
    /**
     * 信息列表
     */
    private ArrayList<T> list;
    /**
     * 本次查询的最后一条记录的定位值, 要跟排序字段一一对应, 要包含能唯一标识一条记录的字段. 用以滚动查询的 paginator 入参
     * 实际上应该是一个 tuple 类型
     */
    private Object[] searchAfter;


    public SearchAfterPagedList(long totalCount, int pageSize, Object[] searchAfter, ArrayList<T> list) {
        this.totalCount = totalCount;
        this.searchAfter = searchAfter;
        this.pageSize = pageSize;
        this.list = list;
    }

}
