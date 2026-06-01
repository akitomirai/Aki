package edu.jxust.agritrace.module.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.support.AuthenticatedIntegrationTestSupport;
import edu.jxust.agritrace.module.log.mapper.OperationAuditLogMapper;
import edu.jxust.agritrace.module.log.mapper.po.OperationAuditLogPO;
import edu.jxust.agritrace.module.batch.mapper.BizAttachmentMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BizAttachmentPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BatchControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BizAttachmentMapper bizAttachmentMapper;

    @Autowired
    private TraceBatchMapper traceBatchMapper;

    @Autowired
    private OperationAuditLogMapper operationAuditLogMapper;

    @Test
    void shouldRejectBatchCreationWithoutExplicitCompanyId() throws Exception {
        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "ROUND6-CONTROLLER-001",
                                  "productId": 1,
                                  "originPlace": "Ganzhou Xinfeng",
                                  "productionDate": "2026-03-24"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("companyId")));
    }

    @Test
    void shouldRejectBatchCreationWithoutExplicitProductId() throws Exception {
        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "ROUND6-CONTROLLER-002",
                                  "companyId": 1,
                                  "originPlace": "Ganzhou Xinfeng",
                                  "productionDate": "2026-03-24"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("productId")));
    }

    @Test
    void shouldUploadTraceImageAndBindToTraceRecord() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "trace.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "trace-image".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "trace-image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].businessType").value("trace-image"))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long fileId = uploadJson.path("data").get(0).path("id").asLong();
        String fileUrl = uploadJson.path("data").get(0).path("fileUrl").asText();
        String filePath = fileUrl.replace("http://127.0.0.1:8080", "");

        mockMvc.perform(post("/api/batches/1/records/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "PRODUCE",
                                  "title": "field note",
                                  "eventTime": "2026-03-24T13:20",
                                  "operatorName": "trace tester",
                                  "location": "orchard",
                                  "summary": "trace record with image",
                                  "attachmentIds": [%d],
                                  "visibleToConsumer": true
                                }
                                """.formatted(fileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trace.recentRecords[0].attachments[0].id").value(fileId))
                .andExpect(jsonPath("$.data.trace.recentRecords[0].imageUrl").value(fileUrl));

        mockMvc.perform(get(filePath))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUploadQualityAttachmentAndBindToReport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "quality-report.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "quality-attachment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].businessType").value("quality-attachment"))
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long fileId = uploadJson.path("data").get(0).path("id").asLong();
        String fileUrl = uploadJson.path("data").get(0).path("fileUrl").asText();
        String filePath = fileUrl.replace("http://127.0.0.1:8080", "");

        mockMvc.perform(post("/api/batches/1/quality-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportNo": "QA-ROUND6-UPLOADED",
                                  "agency": "Jiangxi Quality Center",
                                  "result": "PASS",
                                  "reportTime": "2026-03-24T14:30",
                                  "highlights": ["pass", "file uploaded"],
                                  "attachmentIds": [%d]
                                }
                                """.formatted(fileId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quality.latestReport.attachments[0].id").value(fileId))
                .andExpect(jsonPath("$.data.quality.latestReport.attachments[0].fileUrl").value(fileUrl));

        mockMvc.perform(get(filePath))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectEmptyFileUpload() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "files",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(empty)
                        .param("businessType", "trace-image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be empty")));
    }

    @Test
    void shouldRejectUnsupportedTraceImageType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "trace.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "trace-image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("supports")));
    }

    @Test
    void shouldRejectOversizedTraceImage() throws Exception {
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        Arrays.fill(bytes, (byte) 1);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "large.png",
                MediaType.IMAGE_PNG_VALUE,
                bytes
        );

        mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "trace-image"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("5 MB")));
    }

    @Test
    void shouldCleanupExpiredOrphanAttachments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "cleanup.png",
                MediaType.IMAGE_PNG_VALUE,
                "cleanup".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "trace-image"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long fileId = uploadJson.path("data").get(0).path("id").asLong();

        BizAttachmentPO attachmentPO = bizAttachmentMapper.selectById(fileId);
        attachmentPO.setCreatedAt(LocalDateTime.now().minusHours(3));
        bizAttachmentMapper.updateById(attachmentPO);

        mockMvc.perform(post("/api/batches/files/cleanup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cleanedCount").value(1))
                .andExpect(jsonPath("$.data.cleanedIds[0]").value(fileId));
    }

    @Test
    void shouldAllowRegulatorToReadBatchListAndWorkbench() throws Exception {
        authenticateAs(REGULATOR_SESSION);

        mockMvc.perform(get("/api/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].batchCode").isNotEmpty())
                .andExpect(jsonPath("$.data[0].productCode").isNotEmpty())
                .andExpect(jsonPath("$.data[0].companyName").isNotEmpty())
                .andExpect(jsonPath("$.data[0].qualityStatus").isNotEmpty())
                .andExpect(jsonPath("$.data[0].taskStatusLabel").isNotEmpty());

        mockMvc.perform(get("/api/batches")
                        .param("productName", "ORANGE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productCode").value("ORANGE-001"));

        mockMvc.perform(get("/api/batches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch.batchCode").value("BATCH20260311001"))
                .andExpect(jsonPath("$.data.product.productCode").value("ORANGE-001"))
                .andExpect(jsonPath("$.data.company.name").isNotEmpty())
                .andExpect(jsonPath("$.data.status.label").isNotEmpty())
                .andExpect(jsonPath("$.data.quality.label").isNotEmpty())
                .andExpect(jsonPath("$.data.trace.recentRecords[0].title").isNotEmpty())
                .andExpect(jsonPath("$.data.actions[?(@.code=='ADD_TRACE')].enabled", everyItem(is(false))))
                .andExpect(jsonPath("$.data.actions[?(@.code=='UPLOAD_QUALITY')].enabled", everyItem(is(false))))
                .andExpect(jsonPath("$.data.actions[?(@.code=='GENERATE_QR')].enabled", everyItem(is(false))))
                .andExpect(jsonPath("$.data.actions[?(@.code=='PUBLISH')].enabled", everyItem(is(false))))
                .andExpect(jsonPath("$.data.actions[?(@.code=='FREEZE')].enabled", everyItem(is(false))))
                .andExpect(jsonPath("$.data.actions[?(@.code=='RECALL')].enabled", everyItem(is(false))));
    }

    @Test
    void shouldRestrictFieldDraftsToAssignedOperator() throws Exception {
        authenticateAs(OPERATOR_SESSION);

        mockMvc.perform(post("/api/batches/1/field-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "PRODUCE",
                                  "title": "field draft",
                                  "eventTime": "2026-03-24T09:30",
                                  "operatorName": "Field Operator",
                                  "location": "orchard",
                                  "summary": "draft before submit",
                                  "attachmentIds": [],
                                  "uploadedFiles": [],
                                  "visibleToConsumer": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId").value(1))
                .andExpect(jsonPath("$.data.operatorName").value("Field Operator"));

        mockMvc.perform(get("/api/batches/field-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].batchId").value(1))
                .andExpect(jsonPath("$.data[0].summary").value("draft before submit"));

        mockMvc.perform(delete("/api/batches/1/field-draft"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/batches/1/field-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "QUALITY",
                                  "operatorName": "Field Operator",
                                  "location": "orchard",
                                  "summary": "operator should not save quality stage draft"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("现场操作员只能补录生产、运输、仓储、发运、上市环节。"));

        mockMvc.perform(post("/api/batches/1/records/quick")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "REGULATION",
                                  "title": "regulator stage should be blocked",
                                  "eventTime": "2026-03-24T09:30",
                                  "operatorName": "Field Operator",
                                  "location": "orchard",
                                  "summary": "operator should not submit regulator stage trace",
                                  "attachmentIds": [],
                                  "visibleToConsumer": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("现场操作员只能补录生产、运输、仓储、发运、上市环节。"));

        long staleDraftBatchId = createDraftBatch("FIELD-DRAFT-STALE-" + System.nanoTime());
        TraceBatchPO staleDraftBatchPO = traceBatchMapper.selectById(staleDraftBatchId);
        staleDraftBatchPO.setAssigneeUserId(OPERATOR_SESSION.userId());
        staleDraftBatchPO.setCompanyId(OPERATOR_SESSION.companyId());
        traceBatchMapper.updateById(staleDraftBatchPO);

        authenticateAs(OPERATOR_SESSION);
        mockMvc.perform(post("/api/batches/" + staleDraftBatchId + "/field-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "PRODUCE",
                                  "operatorName": "Field Operator",
                                  "location": "orchard",
                                  "summary": "stale draft should be hidden after reassignment"
                                }
                                """))
                .andExpect(status().isOk());

        staleDraftBatchPO.setAssigneeUserId(null);
        traceBatchMapper.updateById(staleDraftBatchPO);

        mockMvc.perform(get("/api/batches/field-drafts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.batchId==" + staleDraftBatchId + ")]").isEmpty());

        long unassignedBatchId = createDraftBatch("FIELD-DRAFT-UNASSIGNED-" + System.nanoTime());
        authenticateAs(OPERATOR_SESSION);

        mockMvc.perform(post("/api/batches/" + unassignedBatchId + "/field-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "PRODUCE",
                                  "operatorName": "Field Operator",
                                  "location": "orchard",
                                  "summary": "operator should not draft unassigned batch"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前批次未分配给你，不能查看或提交现场记录。"));
    }

    @Test
    void shouldRejectAdminAndRegulatorFieldEntryWrites() throws Exception {
        long beforePlatformDraftDenied = countDeniedLogs("TRACE_RECORD_DENIED", PLATFORM_ADMIN_SESSION.userId(), "现场作业草稿仅限当前批次分配的现场操作员维护。");
        long beforeRegulatorUploadDenied = countDeniedLogs("TRACE_RECORD_DENIED", REGULATOR_SESSION.userId(), "当前账号不能上传现场图片。");

        authenticateAs(PLATFORM_ADMIN_SESSION);
        mockMvc.perform(post("/api/batches/1/field-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stage": "PRODUCE",
                                  "operatorName": "Platform Admin",
                                  "location": "orchard",
                                  "summary": "admin should manage trace records in workbench"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("现场作业草稿仅限当前批次分配的现场操作员维护。"));

        authenticateAs(REGULATOR_SESSION);
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "regulator-trace.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "trace-image".getBytes()
        );
        mockMvc.perform(multipart("/api/batches/files/upload")
                        .file(file)
                        .param("businessType", "trace-image"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号不能上传现场图片。"));

        assertThat(countDeniedLogs("TRACE_RECORD_DENIED", PLATFORM_ADMIN_SESSION.userId(), "现场作业草稿仅限当前批次分配的现场操作员维护。"))
                .isEqualTo(beforePlatformDraftDenied + 1);
        assertThat(countDeniedLogs("TRACE_RECORD_DENIED", REGULATOR_SESSION.userId(), "当前账号不能上传现场图片。"))
                .isEqualTo(beforeRegulatorUploadDenied + 1);
    }

    @Test
    void shouldVerifyTraceHashChainForBatch() throws Exception {
        mockMvc.perform(get("/api/batches/1/trace-chain/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.statusLabel").value("校验通过"))
                .andExpect(jsonPath("$.data.totalRecords").value(2))
                .andExpect(jsonPath("$.data.latestHash").isNotEmpty());
    }

    @Test
    void shouldRejectRegulatorWriteOperationsAndRecordDeniedLogs() throws Exception {
        String batchCode = "REG-READONLY-" + System.nanoTime();
        long batchId = createDraftBatch(batchCode);

        long beforeQrGenerateDenied = countDeniedLogs("QR_PUBLISH_DENIED", REGULATOR_SESSION.userId(), "当前账号不能执行“生成二维码”操作。");
        long beforePublishDenied = countDeniedLogs("QR_PUBLISH_DENIED", REGULATOR_SESSION.userId(), "当前账号不能执行“发布批次”操作。");
        long beforeRiskDenied = countDeniedLogs("RISK_ACTION_DENIED", REGULATOR_SESSION.userId(), "当前账号不能处理风险动作。");

        authenticateAs(REGULATOR_SESSION);

        mockMvc.perform(post("/api/batches/" + batchId + "/qr"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号不能执行“生成二维码”操作。"));

        mockMvc.perform(post("/api/batches/" + batchId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus": "PUBLISHED",
                                  "reason": "监管账号不应能发布批次",
                                  "operatorName": "Regulator"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号不能执行“发布批次”操作。"));

        mockMvc.perform(post("/api/batches/" + batchId + "/risk-actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionType": "COMMENT",
                                  "reason": "监管账号不应能写风险动作",
                                  "comment": "只读验证",
                                  "operatorName": "Regulator"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前账号不能处理风险动作。"));

        assertThat(countDeniedLogs("QR_PUBLISH_DENIED", REGULATOR_SESSION.userId(), "当前账号不能执行“生成二维码”操作。"))
                .isEqualTo(beforeQrGenerateDenied + 1);
        assertThat(countDeniedLogs("QR_PUBLISH_DENIED", REGULATOR_SESSION.userId(), "当前账号不能执行“发布批次”操作。"))
                .isEqualTo(beforePublishDenied + 1);
        assertThat(countDeniedLogs("RISK_ACTION_DENIED", REGULATOR_SESSION.userId(), "当前账号不能处理风险动作。"))
                .isEqualTo(beforeRiskDenied + 1);

        OperationAuditLogPO publishDeniedLog = findLatestDeniedLog("QR_PUBLISH_DENIED", REGULATOR_SESSION.userId(), "当前账号不能执行“发布批次”操作。");
        assertThat(publishDeniedLog).isNotNull();
        assertThat(publishDeniedLog.getRoleCode()).isEqualTo("REGULATOR");
        assertThat(publishDeniedLog.getTargetId()).isEqualTo(batchId);
        assertThat(publishDeniedLog.getTargetName()).isEqualTo(batchCode);

        OperationAuditLogPO riskDeniedLog = findLatestDeniedLog("RISK_ACTION_DENIED", REGULATOR_SESSION.userId(), "当前账号不能处理风险动作。");
        assertThat(riskDeniedLog).isNotNull();
        assertThat(riskDeniedLog.getRoleCode()).isEqualTo("REGULATOR");
        assertThat(riskDeniedLog.getTargetId()).isEqualTo(batchId);
        assertThat(riskDeniedLog.getTargetName()).isEqualTo(batchCode);
    }

    private long createDraftBatch(String batchCode) throws Exception {
        authenticateAs(PLATFORM_ADMIN_SESSION);
        MvcResult result = mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "%s",
                                  "companyId": 1,
                                  "productId": 1,
                                  "originPlace": "Jiangxi Ganzhou Xinfeng Orchard",
                                  "productionDate": "2026-03-24",
                                  "publicRemark": "regulator readonly smoke",
                                  "internalRemark": "regulator readonly smoke"
                                }
                                """.formatted(batchCode)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("batch").path("id").asLong();
    }

    private long countDeniedLogs(String actionType, Long operatorUserId, String summary) {
        return operationAuditLogMapper.selectCount(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, actionType)
                .eq(operatorUserId != null, OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(summary != null, OperationAuditLogPO::getSummary, summary)
                .eq(OperationAuditLogPO::getResult, "FAILED"));
    }

    private OperationAuditLogPO findLatestDeniedLog(String actionType, Long operatorUserId, String summary) {
        return operationAuditLogMapper.selectOne(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, actionType)
                .eq(operatorUserId != null, OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(summary != null, OperationAuditLogPO::getSummary, summary)
                .eq(OperationAuditLogPO::getResult, "FAILED")
                .orderByDesc(OperationAuditLogPO::getId)
                .last("limit 1"));
    }
}
