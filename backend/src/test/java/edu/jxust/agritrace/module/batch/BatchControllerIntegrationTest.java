package edu.jxust.agritrace.module.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectBatchCreationWithoutExplicitCompanyId() throws Exception {
        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": "ROUND5-CONTROLLER-001",
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
                                  "batchCode": "ROUND5-CONTROLLER-002",
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
                                  "reportNo": "QA-ROUND5-UPLOADED",
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
}
