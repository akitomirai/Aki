package edu.jxust.agritrace.module.publictrace;

import edu.jxust.agritrace.module.batch.service.BatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
                .andExpect(jsonPath("$.data.summary.productName").value("赣南脐橙"))
                .andExpect(jsonPath("$.data.summary.batchCode").value("BATCH20260311001"))
                .andExpect(jsonPath("$.data.timeline").isArray())
                .andExpect(jsonPath("$.data.quality").exists())
                .andExpect(jsonPath("$.data.company").exists())
                .andExpect(jsonPath("$.data.risk.hasRisk").value(false));

        long afterPv = batchService.getBatchWorkbench(1L).qr().pv();
        long afterUv = batchService.getBatchWorkbench(1L).qr().uv();

        org.junit.jupiter.api.Assertions.assertEquals(beforePv + 1, afterPv);
        org.junit.jupiter.api.Assertions.assertTrue(afterUv >= 1);
    }
}
