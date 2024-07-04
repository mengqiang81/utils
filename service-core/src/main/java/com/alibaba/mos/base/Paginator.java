package com.alibaba.mos.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 该类是当查询歌曲时用于分页展示的页面导航器
 *
 * @author Li Hongzhen
 */
@Getter
@Setter
public class Paginator implements Serializable {
    private static final long serialVersionUID = -863096303106093676L;

    /**
     * 默认条目数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * 默认第一页的页码
     */
    public static final int DEFAULT_FIRST_PAGE = 1;
    /**
     * 每页显示多少条记录
     */
    private int pageSize = Paginator.DEFAULT_PAGE_SIZE;
    /**
     * 当前页码
     */
    private long page = Paginator.DEFAULT_FIRST_PAGE;
    /**
     * 排序
     */
    private List<Sort> sort;
    /**
     * 滚动查询Id
     */
    private String scrollId;


    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? 1 : pageSize;
    }

    public long offset() {
        return (page - 1) * pageSize;
    }

    public static Paginator page(long page) {
        Paginator paginator = new Paginator();
        paginator.setPage(page);
        return paginator;
    }

    public static Paginator page(long page, int pageSize) {
        Paginator paginator = new Paginator();
        paginator.setPage(page);
        paginator.setPageSize(pageSize);
        return paginator;
    }

}
