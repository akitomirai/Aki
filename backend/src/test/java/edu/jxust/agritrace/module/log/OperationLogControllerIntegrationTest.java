package edu.jxust.agritrace.module.log;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OperationLogControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    private static final AtomicLong ID_SEQ = new AtomicLong(10_000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OperationAuditLogMapper operationAuditLogMapper;

    @Test
    void shouldAllowPlatformAdminToReadLogsAcrossCompanies() throws Exception {
        long otherCompanyId = createCompany("Scoped Log Test Company");
        String operatorKeyword = "scope-log-" + ID_SEQ.incrementAndGet();

        recordLog(
                null,
                operatorKeyword,
                "PLATFORM_ADMIN",
                1L,
                "USER_UPDATE",
                "USER",
                101L,
                "Own company log",
                "SUCCESS",
                "Own company log"
        );
        recordLog(
                null,
                operatorKeyword,
                "PLATFORM_ADMIN",
                otherCompanyId,
                "USER_UPDATE",
                "USER",
                202L,
                "Other company log",
                "SUCCESS",
                "Other company log"
        );

        authenticateAs(PLATFORM_ADMIN_SESSION);

        mockMvc.perform(get("/api/logs")
                        .param("operatorKeyword", operatorKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[*].companyId", hasItems(1, (int) otherCompanyId)));
    }

    @Test
    void shouldRestrictEnterpriseAdminToOwnCompanyLogs() throws Exception {
        long otherCompanyId = createCompany("Scoped Log Test Company");
        String operatorKeyword = "scope-log-" + ID_SEQ.incrementAndGet();

        recordLog(
                null,
                operatorKeyword,
                "PLATFORM_ADMIN",
                1L,
                "USER_UPDATE",
                "USER",
                101L,
                "Own company log",
                "SUCCESS",
                "Own company log"
        );
        recordLog(
                null,
                operatorKeyword,
                "PLATFORM_ADMIN",
                otherCompanyId,
                "USER_UPDATE",
                "USER",
                202L,
                "Other company log",
                "SUCCESS",
                "Other company log"
        );

        authenticateAs(ENTERPRISE_ADMIN_SESSION);

        mockMvc.perform(get("/api/logs")
                        .param("operatorKeyword", operatorKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].companyId").value(1))
                .andExpect(jsonPath("$.data.items[0].companyName").value("Demo Orchard Company"))
                .andExpect(jsonPath("$.data.items[0].targetName").value("Own company log"));
    }

    @Test
    void shouldRecordDeniedLogWhenEnterpriseAdminRequestsAnotherCompany() throws Exception {
        long otherCompanyId = createCompany("Forbidden Log Scope Company");
        long beforeCount = countDeniedLogs(ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业日志。");

        authenticateAs(ENTERPRISE_ADMIN_SESSION);

        mockMvc.perform(get("/api/logs")
                        .param("companyId", String.valueOf(otherCompanyId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能查看本企业日志。"));

        assertThat(countDeniedLogs(ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业日志。"))
                .isEqualTo(beforeCount + 1);

        OperationAuditLogPO deniedLog = findLatestDeniedLog(ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业日志。");
        assertThat(deniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(deniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(deniedLog.getTargetType()).isEqualTo("LOG");
        assertThat(deniedLog.getResult()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectOperatorAndRecordDeniedLog() throws Exception {
        long beforeCount = countDeniedLogs(OPERATOR_SESSION.userId(), "你没有查看操作日志的权限。");

        authenticateAs(OPERATOR_SESSION);

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你没有查看操作日志的权限。"));

        assertThat(countDeniedLogs(OPERATOR_SESSION.userId(), "你没有查看操作日志的权限。"))
                .isEqualTo(beforeCount + 1);

        OperationAuditLogPO deniedLog = findLatestDeniedLog(OPERATOR_SESSION.userId(), "你没有查看操作日志的权限。");
        assertThat(deniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(deniedLog.getRoleCode()).isEqualTo("OPERATOR");
        assertThat(deniedLog.getTargetType()).isEqualTo("LOG");
        assertThat(deniedLog.getResult()).isEqualTo("FAILED");
    }

    private long createCompany(String name) {
        long companyId = ID_SEQ.incrementAndGet();
        jdbcTemplate.update(
                """
                        INSERT INTO org_company (id, name, license_no, address, contact, phone, status, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, 'ENABLED', ?)
                        """,
                companyId,
                name,
                "LIC-" + companyId,
                "Nanchang Test Address " + companyId,
                "Case Contact",
                String.format("139%08d", companyId % 100_000_000),
                LocalDateTime.now()
        );
        return companyId;
    }

    private void recordLog(
            Long operatorUserId,
            String operatorName,
            String roleCode,
            Long companyId,
            String actionType,
            String targetType,
            Long targetId,
            String targetName,
            String result,
            String summary
    ) {
        OperationAuditLogPO logPO = new OperationAuditLogPO();
        logPO.setOperatorUserId(operatorUserId);
        logPO.setOperatorName(operatorName);
        logPO.setRoleCode(roleCode);
        logPO.setCompanyId(companyId);
        logPO.setActionType(actionType);
        logPO.setTargetType(targetType);
        logPO.setTargetId(targetId);
        logPO.setTargetName(targetName);
        logPO.setResult(result);
        logPO.setSummary(summary);
        logPO.setCreatedAt(LocalDateTime.now());
        operationAuditLogMapper.insert(logPO);
    }

    private long countDeniedLogs(Long operatorUserId, String summary) {
        return operationAuditLogMapper.selectCount(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, "LOG_ACCESS_DENIED")
                .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(OperationAuditLogPO::getSummary, summary));
    }

    private OperationAuditLogPO findLatestDeniedLog(Long operatorUserId, String summary) {
        List<OperationAuditLogPO> logs = operationAuditLogMapper.selectList(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, "LOG_ACCESS_DENIED")
                .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(OperationAuditLogPO::getSummary, summary)
                .orderByDesc(OperationAuditLogPO::getId));
        assertThat(logs).isNotEmpty();
        return logs.get(0);
    }
}
