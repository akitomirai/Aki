package edu.jxust.agritrace.module.publictrace;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchRiskActionCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import edu.jxust.agritrace.module.batch.service.BatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicTraceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BatchService batchService;

    @Test
    void shouldReturnPublicTraceStructureAndRecordScan() throws Exception {
        long beforePv = batchService.getBatchWorkbench(1L).qr().pv();

        mockMvc.perform(get("/api/public/traces/test-token-2026")
                        .header("User-Agent", "JUnit-Mobile")
                        .header("Referer", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.productName").exists())
                .andExpect(jsonPath("$.data.summary.batchCode").exists())
                .andExpect(jsonPath("$.data.timeline").isArray())
                .andExpect(jsonPath("$.data.quality").exists())
                .andExpect(jsonPath("$.data.company").exists())
                .andExpect(jsonPath("$.data.risk.status").value("NORMAL"));

        long afterPv = batchService.getBatchWorkbench(1L).qr().pv();
        long afterUv = batchService.getBatchWorkbench(1L).qr().uv();

        org.junit.jupiter.api.Assertions.assertEquals(beforePv + 1, afterPv);
        org.junit.jupiter.api.Assertions.assertTrue(afterUv >= 1);
    }

    @Test
    void shouldReturnFrozenProcessingRiskStructureForPublicTrace() throws Exception {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-PUBLIC-PROCESSING-ROUND6",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "public processing risk test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-PUBLIC-PROCESSING-ROUND6",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T16:00",
                List.of("pass"),
                List.of()
        ));

        String token = batchService.generateQr(batchId).qr().token();
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.PUBLISHED, "ready", "tester"));
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.FROZEN, "investigation running", "tester"));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.COMMENT,
                "investigation running",
                "Handling comments recorded.",
                "tester"
        ));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.PROCESSING,
                "processing",
                "The batch is being handled.",
                "tester"
        ));

        mockMvc.perform(get("/api/public/traces/{token}", token)
                        .header("User-Agent", "JUnit-Mobile")
                        .header("Referer", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk.hasRisk").value(true))
                .andExpect(jsonPath("$.data.risk.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.risk.riskLevel").value("pending"))
                .andExpect(jsonPath("$.data.risk.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.data.timeline.length()", greaterThanOrEqualTo(0)));
    }

    @Test
    void shouldReturnRectifiedThenNormalRiskStructureForPublicTrace() throws Exception {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-PUBLIC-RECTIFIED-ROUND6",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "public rectified risk test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-PUBLIC-RECTIFIED-ROUND6",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T17:00",
                List.of("pass"),
                List.of()
        ));

        String token = batchService.generateQr(batchId).qr().token();
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.PUBLISHED, "ready", "tester"));
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.FROZEN, "risk found", "tester"));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.RECTIFICATION,
                "rectification note",
                "Shelf stock corrected and traced.",
                "tester"
        ));
        batchService.addRiskAction(batchId, new BatchRiskActionCreateRequest(
                RiskActionType.RECTIFIED,
                "rectified",
                "Rectification completed.",
                "tester"
        ));

        mockMvc.perform(get("/api/public/traces/{token}", token)
                        .header("User-Agent", "JUnit-Mobile")
                        .header("Referer", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk.status").value("RECTIFIED"))
                .andExpect(jsonPath("$.data.risk.riskLevel").value("pending"));

        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.PUBLISHED, "resume", "tester"));

        mockMvc.perform(get("/api/public/traces/{token}", token)
                        .header("User-Agent", "JUnit-Mobile")
                        .header("Referer", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk.status").value("NORMAL"))
                .andExpect(jsonPath("$.data.summary.statusLabel").value("Published"));
    }
}
