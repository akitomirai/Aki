package edu.jxust.agritrace.module.batch;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BatchServicePersistenceIntegrationTest {

    @Autowired
    private BatchService batchService;

    @Test
    void shouldPersistBatchStatusTransitions() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-STATE-ROUND4",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "status test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-STATE-ROUND4",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T10:00",
                List.of("sample pass", "ready"),
                List.of()
        ));
        batchService.generateQr(batchId);

        assertEquals("PUBLISHED", batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.PUBLISHED,
                "ready to publish",
                "tester"
        )).status().code());

        assertEquals("FROZEN", batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.FROZEN,
                "freeze for check",
                "tester"
        )).status().code());

        assertEquals("PUBLISHED", batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.PUBLISHED,
                "issue fixed",
                "tester"
        )).status().code());

        var recalled = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.RECALLED,
                "recall flow test",
                "tester"
        ));

        assertEquals("RECALLED", recalled.status().code());
        assertEquals(5, recalled.statusHistory().size());
    }

    @Test
    void shouldKeepQrGenerationIdempotent() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-QR-ROUND4",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "qr test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-QR-ROUND4",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T10:20",
                List.of("publishable"),
                List.of()
        ));

        var first = batchService.generateQr(batchId);
        var second = batchService.generateQr(batchId);

        assertTrue(first.qr().generated());
        assertTrue(second.qr().generated());
        assertEquals(first.qr().id(), second.qr().id());
        assertEquals(first.qr().token(), second.qr().token());
        assertEquals(first.qr().imageUrl(), second.qr().imageUrl());
        assertNotNull(second.qr().publicUrl());
    }

    @Test
    void shouldValidateCompanyAndProductSelection() {
        IllegalArgumentException missingCompany = assertThrows(IllegalArgumentException.class, () ->
                batchService.createBatch(new BatchCreateRequest(
                        "BATCH-INVALID-COMPANY",
                        1L,
                        999L,
                        "Xinfeng",
                        "2026-03-24",
                        null,
                        null
                )));
        assertTrue(missingCompany.getMessage().contains("companyId"));

        IllegalArgumentException missingProduct = assertThrows(IllegalArgumentException.class, () ->
                batchService.createBatch(new BatchCreateRequest(
                        "BATCH-INVALID-PRODUCT",
                        999L,
                        1L,
                        "Xinfeng",
                        "2026-03-24",
                        null,
                        null
                )));
        assertTrue(missingProduct.getMessage().contains("productId"));
    }

    @Test
    void shouldAggregateScanStatsInWorkbench() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-SCAN-ROUND4",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "scan stats test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-SCAN-ROUND4",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T11:00",
                List.of("pass"),
                List.of()
        ));

        var generated = batchService.generateQr(batchId);
        String token = generated.qr().token();

        batchService.recordPublicTraceAccess(token, new PublicTraceAccessContext("127.0.0.1", "JUnit Mobile", "http://127.0.0.1:5173"));
        batchService.recordPublicTraceAccess(token, new PublicTraceAccessContext("127.0.0.1", "JUnit Mobile", "http://127.0.0.1:5173"));
        batchService.recordPublicTraceAccess(token, new PublicTraceAccessContext("127.0.0.2", "JUnit Desktop", "http://127.0.0.1:5173"));

        var workbench = batchService.getBatchWorkbench(batchId);

        assertEquals(3, workbench.scanStats().pv());
        assertEquals(2, workbench.scanStats().uv());
        assertNotNull(workbench.scanStats().lastScanAt());
        assertEquals(7, workbench.scanStats().trend().size());
        assertTrue(workbench.scanStats().recentRecords().size() >= 3);
        assertEquals(1L, workbench.company().id());
        assertEquals(1L, workbench.product().id());
        assertTrue(workbench.qr().generated());
    }
}
