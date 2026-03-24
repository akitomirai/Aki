package edu.jxust.agritrace.module.publictrace;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
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
    void shouldReturnFrozenRiskStructureForPublicTrace() throws Exception {
        Long batchId = batchService.createBatch(new BatchCreateRequest(
                "BATCH-PUBLIC-RISK-ROUND5",
                1L,
                1L,
                "Jiangxi Ganzhou Xinfeng Orchard",
                "2026-03-24",
                "public risk test",
                "test"
        )).batch().id();

        batchService.addQualityReport(batchId, new QualityReportCreateRequest(
                "QA-PUBLIC-RISK-ROUND5",
                "Jiangxi Quality Center",
                "PASS",
                "2026-03-24T16:00",
                List.of("pass"),
                List.of()
        ));

        String token = batchService.generateQr(batchId).qr().token();
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.PUBLISHED, "ready", "tester"));
        batchService.changeStatus(batchId, new BatchStatusActionRequest(BatchStatus.FROZEN, "investigation running", "tester"));

        mockMvc.perform(get("/api/public/traces/{token}", token)
                        .header("User-Agent", "JUnit-Mobile")
                        .header("Referer", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk.hasRisk").value(true))
                .andExpect(jsonPath("$.data.risk.status").value("FROZEN"))
                .andExpect(jsonPath("$.data.risk.riskLevel").value("warning"))
                .andExpect(jsonPath("$.data.risk.reason").value("investigation running"))
                .andExpect(jsonPath("$.data.risk.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.data.timeline.length()", greaterThanOrEqualTo(0)));
    }
}
