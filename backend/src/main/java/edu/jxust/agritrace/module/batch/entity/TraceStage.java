package edu.jxust.agritrace.module.batch.entity;

public enum TraceStage {
    ARCHIVE("企业建档"),
    PRODUCE("生产记录"),
    QUALITY("质检节点"),
    TRANSPORT("运输流转"),
    WAREHOUSE("仓储记录"),
    DELIVERY("出库发运"),
    MARKET("上市销售"),
    REGULATION("监管处理");

    private final String label;

    TraceStage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
