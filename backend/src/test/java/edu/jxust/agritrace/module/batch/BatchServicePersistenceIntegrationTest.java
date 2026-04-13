package edu.jxust.agritrace.module.batch;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchRiskActionCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.CompanySaveRequest;
import edu.jxust.agritrace.module.batch.dto.ProductSaveRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import edu.jxust.agritrace.module.batch.entity.TraceStage;
import edu.jxust.agritrace.module.batch.mapper.TraceEventMapper;
import edu.jxust.agritrace.module.batch.mapper.po.TraceEventPO;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.service.MasterDataService;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import edu.jxust.agritrace.support.AuthenticatedIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BatchServicePersistenceIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private BatchService batchService;

    @Autowired
    private MasterDataService masterDataService;

    @Autowired
    private TraceEventMapper traceEventMapper;

    @Test
    void shouldPersistBatchStatusTransitionsWithRiskClosure() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-STATE-ROUND6",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "status test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-STATE-ROUND6",
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

        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.COMMENT,
                "investigation started",
                "Sampling issue found and handling comments recorded.",
                "tester"
        ));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.PROCESSING,
                "processing",
                "Team is handling the frozen batch.",
                "tester"
        ));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.RECTIFICATION,
                "rectification note",
                "Packaging labels and shelf stock have been corrected.",
                "tester"
        ));
        var rectified = batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.RECTIFIED,
                "rectified",
                "Rectification completed and waiting for resume.",
                "tester"
        ));

        assertEquals("RECTIFIED", rectified.risk().status());
        assertEquals("RECTIFIED", rectified.riskHandling().currentStage());
        assertTrue(rectified.riskHandling().canResume());

        var resumed = batchService.changeStatus(batchId, new BatchStatusActionRequest(
                BatchStatus.PUBLISHED,
                "issue fixed",
                "tester"
        ));

        assertEquals("PUBLISHED", resumed.status().code());
        assertEquals("NORMAL", resumed.risk().status());

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
                "BATCH-QR-ROUND6",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "qr test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-QR-ROUND6",
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
    void shouldPassTraceHashChainAndDetectTampering() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-HASH-ROUND7",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "hash chain demo",
                "test"
        )).batch().id();

        batchService.addTraceRecord(batchId, new TraceRecordCreateRequest(
                TraceStage.PRODUCE,
                "采收建档",
                "2026-03-24T09:10",
                "trace tester",
                "A区果园",
                "完成首条采收记录留痕。",
                null,
                List.of(),
                true
        ));
        batchService.addTraceRecord(batchId, new TraceRecordCreateRequest(
                TraceStage.TRANSPORT,
                "冷链发运",
                "2026-03-24T11:30",
                "trace tester",
                "赣州发运点",
                "完成第二条运输记录留痕。",
                null,
                List.of(),
                true
        ));

        var passed = batchService.verifyTraceChain(batchId);
        assertTrue(passed.passed());
        assertEquals("校验通过", passed.statusLabel());
        assertEquals(2, passed.totalRecords());
        assertNotNull(passed.latestHash());

        TraceEventPO tampered = traceEventMapper.selectById(batchService.getBatchEntityById(batchId).getTraceRecords().get(0).id());
        tampered.setContentJson("{\"summary\":\"该记录已被人为篡改\"}");
        traceEventMapper.updateById(tampered);

        var failed = batchService.verifyTraceChain(batchId);
        assertFalse(failed.passed());
        assertEquals("校验失败", failed.statusLabel());
        assertEquals(tampered.getId(), failed.failedRecordId());
        assertTrue(failed.message().contains("第 1 条记录"));
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
    void shouldValidateProductOwnershipForBatchCreation() {
        Long companyId = masterDataService.createCompany(new CompanySaveRequest(
                "Round6 Ownership Company",
                "LIC-ROUND6-OWNER",
                "Owner",
                "13900000005",
                "Ji'an City",
                "ENABLED"
        )).id();

        Long anotherCompanyId = masterDataService.createCompany(new CompanySaveRequest(
                "Round6 Another Company",
                "LIC-ROUND6-OWNER-2",
                "Owner 2",
                "13900000006",
                "Fuzhou City",
                "ENABLED"
        )).id();

        Long productId = masterDataService.createProduct(new ProductSaveRequest(
                companyId,
                "Ownership Orange",
                "OWN-ORANGE",
                "Fruit",
                "Ji'an Orchard",
                "/images/products/orange-batch.svg",
                "10kg/box",
                "box",
                "ENABLED"
        )).id();

        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class, () ->
                batchService.createBatch(new BatchCreateRequest(
                        "BATCH-OWNERSHIP-ROUND6",
                        productId,
                        anotherCompanyId,
                        "Ji'an Orchard",
                        "2026-03-24",
                        null,
                        null
                )));

        assertTrue(mismatch.getMessage().contains("belong"));
    }

    @Test
    void shouldAggregateScanStatsInWorkbench() {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-SCAN-ROUND6",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "scan stats test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-SCAN-ROUND6",
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

    @Test
    void shouldCreateBatchFromManagedCompanyAndProduct() {
        Long companyId = masterDataService.createCompany(new CompanySaveRequest(
                "Round6 Managed Company",
                "LIC-ROUND6-MANAGED",
                "Delta",
                "13900000007",
                "Yingtan City",
                "ENABLED"
        )).id();

        Long productId = masterDataService.createProduct(new ProductSaveRequest(
                companyId,
                "Round6 Managed Tea",
                "TEA-ROUND6",
                "Tea",
                "Yingtan Tea Base",
                "/images/products/green-tea-batch.svg",
                "200g/box",
                "box",
                "ENABLED"
        )).id();

        var created = batchService.createBatch(new BatchCreateRequest(
                "BATCH-MASTERDATA-ROUND6",
                productId,
                companyId,
                "Yingtan Tea Base",
                "2026-03-24",
                "master data chain",
                "test"
        ));

        assertEquals(companyId, created.company().id());
        assertEquals(productId, created.product().id());
    }
}
