package edu.jxust.agritrace.module.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.module.log.mapper.OperationAuditLogMapper;
import edu.jxust.agritrace.module.log.mapper.po.OperationAuditLogPO;
import edu.jxust.agritrace.support.AuthenticatedIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    private static final String BACKUP_DENIED_MESSAGE = "当前账号没有导出统计备份的权限。";
    private static final String READ_DENIED_MESSAGE = "当前账号没有查看统计分析的权限。";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OperationAuditLogMapper operationAuditLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAllowRegulatorToReadStatisticsButRejectBackupExport() throws Exception {
        authenticateAs(REGULATOR_SESSION);
        long beforeCount = countDeniedLogs(REGULATOR_SESSION.userId(), "DASHBOARD_BACKUP_DENIED", BACKUP_DENIED_MESSAGE);

        mockMvc.perform(get("/api/dashboard/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchTotal").isNumber())
                .andExpect(jsonPath("$.data.publishStatusSummary.recalledOrFrozen").isNumber());

        mockMvc.perform(post("/api/dashboard/backup"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(BACKUP_DENIED_MESSAGE));

        assertThat(countDeniedLogs(REGULATOR_SESSION.userId(), "DASHBOARD_BACKUP_DENIED", BACKUP_DENIED_MESSAGE))
                .isEqualTo(beforeCount + 1);
        OperationAuditLogPO deniedLog = findLatestDeniedLog(REGULATOR_SESSION.userId(), "DASHBOARD_BACKUP_DENIED", BACKUP_DENIED_MESSAGE);
        assertThat(deniedLog.getRoleCode()).isEqualTo("REGULATOR");
        assertThat(deniedLog.getTargetType()).isEqualTo("DASHBOARD");
        assertThat(deniedLog.getResult()).isEqualTo("FAILED");
    }

    @Test
    void shouldRestrictEnterpriseBackupToOwnCompanyAndUseQrPvCounts() throws Exception {
        authenticateAs(ENTERPRISE_ADMIN_SESSION);
        jdbcTemplate.update("UPDATE qr_code SET pv = ? WHERE qr_token = ?", 5, "test-token-2026");

        MvcResult result = mockMvc.perform(post("/api/dashboard/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("企业数据范围：1"))
                .andExpect(jsonPath("$.data.companyTotal").value(1))
                .andExpect(jsonPath("$.data.batches[*].companyName", everyItem(is("Demo Orchard Company"))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("qrQueryTotal").asLong()).isGreaterThanOrEqualTo(5);
        JsonNode targetBatch = findBatchByQrToken(data.path("batches"), "test-token-2026");
        assertThat(targetBatch.path("queryCount").asLong()).isEqualTo(5);
    }

    @Test
    void shouldRejectOperatorStatisticsReadAndRecordDeniedLog() throws Exception {
        authenticateAs(OPERATOR_SESSION);
        long beforeCount = countDeniedLogs(OPERATOR_SESSION.userId(), "DASHBOARD_ACCESS_DENIED", READ_DENIED_MESSAGE);

        mockMvc.perform(get("/api/dashboard/statistics"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(READ_DENIED_MESSAGE));

        assertThat(countDeniedLogs(OPERATOR_SESSION.userId(), "DASHBOARD_ACCESS_DENIED", READ_DENIED_MESSAGE))
                .isEqualTo(beforeCount + 1);
    }

    private long countDeniedLogs(Long operatorUserId, String actionType, String summary) {
        return operationAuditLogMapper.selectCount(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, actionType)
                .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(OperationAuditLogPO::getSummary, summary)
                .eq(OperationAuditLogPO::getResult, "FAILED"));
    }

    private OperationAuditLogPO findLatestDeniedLog(Long operatorUserId, String actionType, String summary) {
        List<OperationAuditLogPO> logs = operationAuditLogMapper.selectList(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, actionType)
                .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(OperationAuditLogPO::getSummary, summary)
                .eq(OperationAuditLogPO::getResult, "FAILED")
                .orderByDesc(OperationAuditLogPO::getId));
        assertThat(logs).isNotEmpty();
        return logs.get(0);
    }

    private JsonNode findBatchByQrToken(JsonNode batches, String qrToken) {
        for (JsonNode batch : batches) {
            if (qrToken.equals(batch.path("qrToken").asText())) {
                return batch;
            }
        }
        throw new AssertionError("Expected backup batch with qrToken " + qrToken);
    }
}
