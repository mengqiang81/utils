package com.alibaba.mos.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 支持滚动查询的分页条件构造类
 * @author chigui
 */
@Getter
@Setter
public class SearchAfterPaginator implements Serializable{
    private static final long serialVersionUID = -2054394112915087164L;

    /**
     * 默认条目数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 每页显示多少条记录
     */
    private int pageSize = SearchAfterPaginator.DEFAULT_PAGE_SIZE;
    /**
     * 上次查询的最后一条记录的定位值, 要跟排序字段一一对应, 要包含能唯一标识一条记录的字段. 如果为空是查第一页
     * 实际上应该是一个 tuple 类型
     */
    private Object[] searchAfter;

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize < 1 ? 1 : pageSize;
    }

    public static SearchAfterPaginator page(int pageSize) {
        SearchAfterPaginator paginator = new SearchAfterPaginator();
        paginator.setPageSize(pageSize);
        return paginator;
    }

    public static SearchAfterPaginator page(Object[] searchAfter) {
        SearchAfterPaginator paginator = new SearchAfterPaginator();
        paginator.setSearchAfter(searchAfter);
        return paginator;
    }

    public static SearchAfterPaginator page(Object[] searchAfter, int pageSize) {
        SearchAfterPaginator paginator = new SearchAfterPaginator();
        paginator.setSearchAfter(searchAfter);
        paginator.setPageSize(pageSize);
        return paginator;
    }

}
