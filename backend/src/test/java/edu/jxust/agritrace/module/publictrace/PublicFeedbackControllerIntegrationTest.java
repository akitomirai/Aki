package edu.jxust.agritrace.module.publictrace;

import edu.jxust.agritrace.support.AuthenticatedIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PublicFeedbackControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptPublicFeedbackAndExposeItToPlatformReader() throws Exception {
        String content = """
                {
                  "productName": "大米",
                  "batchNo": "RICE-202604-R1",
                  "traceCode": "rice-202604-r1",
                  "feedbackType": "信息不一致",
                  "contact": "13800001111",
                  "content": "公开页面的包装标签和实物标签略有差异，请企业核对后更新展示。",
                  "createdAt": "2026-06-01T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/public/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true));

        authenticateAs(PLATFORM_ADMIN_SESSION);
        mockMvc.perform(get("/api/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data[0].content").value("公开页面的包装标签和实物标签略有差异，请企业核对后更新展示。"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void shouldRejectInvalidPublicFeedbackPayload() throws Exception {
        mockMvc.perform(post("/api/public/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feedbackType": "其他",
                                  "content": "太短"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldLetEnterpriseHandleOwnCompanyFeedbackAndRejectRegulatorWrite() throws Exception {
        authenticateAs(ENTERPRISE_ADMIN_SESSION);
        mockMvc.perform(patch("/api/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED",
                                  "handleResult": "已核对公开页展示内容，并同步企业内部处理记录。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.handlerName").value("Enterprise Admin"));

        authenticateAs(REGULATOR_SESSION);
        mockMvc.perform(patch("/api/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "PROCESSING",
                                  "handleResult": "监管只能查看，不能处理。"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldRejectInvalidFeedbackHandlePayload() throws Exception {
        authenticateAs(PLATFORM_ADMIN_SESSION);
        mockMvc.perform(patch("/api/feedback/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE",
                                  "handleResult": "状态非法。"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
