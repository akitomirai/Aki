package edu.jxust.agritrace.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int pageNo;
    private int pageSize;
    private int pages;

    public static <T> PageResult<T> of(List<T> records, long total, int pageNo, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(total);
        r.setPageNo(pageNo);
        r.setPageSize(pageSize);

        int pages = 0;
        if (pageSize > 0) {
            pages = (int) ((total + pageSize - 1) / pageSize);
        }
        r.setPages(pages);
        return r;
    }
}
