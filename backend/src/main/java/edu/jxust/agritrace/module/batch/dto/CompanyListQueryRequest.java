package edu.jxust.agritrace.module.batch.dto;

public class CompanyListQueryRequest {

    private String keyword;
    private String status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
