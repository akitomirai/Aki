package edu.jxust.agritrace.module.batch;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BatchServicePersistenceIntegrationTest {

    @Autowired
    private BatchService batchService;

    @Test
    void shouldPersistBatchStatusTransitions() {
        BatchWorkbenchVO created = batchService.createBatch(new BatchCreateRequest(
                "BATCH-STATE-ROUND3",
                "状态流转测试橙",
                "水果",
                "状态流转测试企业",
                "江西省赣州市信丰县",
                "2026-03-24",
                "用于验证状态流转。",
                "test"
        ));
        Long batchId = created.batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-STATE-ROUND3",
                "江西省农产品质量检测中心",
                "PASS",
                "2026-03-24T10:00",
                List.of("抽检通过", "适合发布")
        ));
        batchService.generateQr(batchId);

        BatchWorkbenchVO published = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.PUBLISHED,
                "质检和二维码已准备完成。",
                "测试操作员"
        ));
        assertEquals("PUBLISHED", published.status().code());

        BatchWorkbenchVO frozen = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.FROZEN,
                "发现异常，先冻结处理。",
                "测试操作员"
        ));
        assertEquals("FROZEN", frozen.status().code());

        BatchWorkbenchVO resumed = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.PUBLISHED,
                "异常已处理，恢复发布。",
                "测试操作员"
        ));
        assertEquals("PUBLISHED", resumed.status().code());

        BatchWorkbenchVO recalled = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.RECALLED,
                "模拟召回流程。",
                "测试操作员"
        ));
        assertEquals("RECALLED", recalled.status().code());
        assertEquals(5, recalled.statusHistory().size());
    }

    @Test
    void shouldKeepQrGenerationIdempotent() {
        BatchWorkbenchVO created = batchService.createBatch(new BatchCreateRequest(
                "BATCH-QR-ROUND3",
                "二维码幂等测试茶",
                "茶叶",
                "二维码测试企业",
                "江西省上饶市婺源县",
                "2026-03-24",
                "用于验证二维码幂等。",
                "test"
        ));
        Long batchId = created.batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-QR-ROUND3",
                "江西省农产品质量检测中心",
                "PASS",
                "2026-03-24T10:20",
                List.of("可发布")
        ));

        BatchWorkbenchVO first = batchService.generateQr(batchId);
        BatchWorkbenchVO second = batchService.generateQr(batchId);

        assertTrue(first.qr().generated());
        assertTrue(second.qr().generated());
        assertEquals(first.qr().id(), second.qr().id());
        assertEquals(first.qr().token(), second.qr().token());
        assertEquals(first.qr().imageUrl(), second.qr().imageUrl());
        assertNotNull(second.qr().publicUrl());
    }
}
