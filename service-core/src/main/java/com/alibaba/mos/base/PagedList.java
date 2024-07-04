package com.alibaba.mos.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 支持分页的结果集，内含总条目数，当前页码，每页条目数等信息
 *
 * @author Richard  Zhang
 */
@Getter
@Setter
@NoArgsConstructor
public class PagedList<T> implements Serializable{
    private static final long serialVersionUID = 7977104388217815130L;
    /**
     * 是不是首页
     */
    private boolean first;
    /**
     * 是不是末页
     */
    private boolean last;
    /**
     * 总页数
     */
    private long totalPage;
    /**
     * 信息总数
     */
    private long totalCount;
    /**
     * 当前页数
     */
    private long currentPage;
    /**
     * 每页的信息条数
     */
    private int pageSize;
    /**
     * 信息列表
     */
    private List<T> list;
    /**
     * 滚动查询Id
     */
    private String scrollId;

    public PagedList(long totalCount, long currentPage, int pageSize, List<T> list) {
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.list = list;
    }

    public PagedList(long totalCount, Paginator paginator, List<T> list) {
        this.totalCount = totalCount;
        this.currentPage = paginator.getPage();
        this.pageSize = paginator.getPageSize();
        this.list = list;
    }

    public long getTotalPage() {
        if (totalPage == 0) {
            if (this.totalCount <= 0) {
                totalPage = 1;
            } else {
                totalPage = this.totalCount % this.pageSize > 0 ? (new BigDecimal(this.totalCount / this.pageSize).setScale(0, BigDecimal.ROUND_FLOOR)).longValue() + 1L : (new BigDecimal(this.totalCount / this.pageSize).setScale(0, BigDecimal.ROUND_FLOOR)).longValue();
            }
        }
        return totalPage;
    }
}
