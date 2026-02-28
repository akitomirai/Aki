package edu.jxust.agritrace.vo;

import edu.jxust.agritrace.entity.Product;
import edu.jxust.agritrace.entity.ProductBatch;
import edu.jxust.agritrace.entity.ProductionRecord;
import lombok.Data;

import java.util.List;

@Data
public class TraceResultVO {

    private ProductBatch batch;
    private Product product;
    private List<ProductionRecord> records;

    public ProductBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductBatch batch) {
        this.batch = batch;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<ProductionRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ProductionRecord> records) {
        this.records = records;
    }
}
