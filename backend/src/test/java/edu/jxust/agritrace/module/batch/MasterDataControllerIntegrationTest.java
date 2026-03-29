package edu.jxust.agritrace.module.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MasterDataControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateUpdateArchiveAndDeleteCompany() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round6 Company",
                                  "licenseNo": "LIC-ROUND6-COMPANY",
                                  "contactPerson": "Alice",
                                  "contactPhone": "13900000001",
                                  "address": "Nanchang City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Round6 Company"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long companyId = created.path("data").path("id").asLong();

        mockMvc.perform(patch("/api/companies/{companyId}", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round6 Company Updated",
                                  "licenseNo": "LIC-ROUND6-COMPANY",
                                  "contactPerson": "Alice Updated",
                                  "contactPhone": "13900000002",
                                  "address": "Ganzhou City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Round6 Company Updated"))
                .andExpect(jsonPath("$.data.contactPerson").value("Alice Updated"));

        mockMvc.perform(post("/api/companies/{companyId}/status", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/companies")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ARCHIVED"));

        mockMvc.perform(delete("/api/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Company deleted."));
    }

    @Test
    void shouldRejectDeletingReferencedCompany() throws Exception {
        mockMvc.perform(delete("/api/companies/{companyId}", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("referenced")));
    }

    @Test
    void shouldCreateUpdateArchiveAndDeleteProductWithCompanyFilter() throws Exception {
        long companyId = objectMapper.readTree(mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round6 Product Company",
                                  "licenseNo": "LIC-ROUND6-PRODUCT",
                                  "contactPerson": "Bob",
                                  "contactPhone": "13900000003",
                                  "address": "Yichun City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();

        MvcResult createProductResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Round6 Rice",
                                  "productCode": "RICE-ROUND6",
                                  "category": "Grain",
                                  "originPlace": "Yichun",
                                  "coverImage": "/images/products/rice-batch.svg",
                                  "specification": "25kg/bag",
                                  "unit": "bag",
                                  "status": "ENABLED"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(companyId))
                .andReturn();

        long productId = objectMapper.readTree(createProductResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Round6 Rice Premium",
                                  "productCode": "RICE-ROUND6",
                                  "category": "Grain",
                                  "originPlace": "Yichun High-standard Farm",
                                  "coverImage": "/images/products/rice-batch.svg",
                                  "specification": "20kg/bag",
                                  "unit": "bag",
                                  "status": "ENABLED"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Round6 Rice Premium"))
                .andExpect(jsonPath("$.data.originPlace").value("Yichun High-standard Farm"));

        mockMvc.perform(post("/api/products/{productId}/status", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ARCHIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/products")
                        .param("companyId", String.valueOf(companyId))
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyId").value(companyId))
                .andExpect(jsonPath("$.data[0].status").value("ARCHIVED"));

        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted."));
    }

    @Test
    void shouldRejectDeletingReferencedProduct() throws Exception {
        mockMvc.perform(delete("/api/products/{productId}", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("referenced")));
    }

    @Test
    void shouldRejectProductCreationWhenCompanyIsNotAvailable() throws Exception {
        long companyId = objectMapper.readTree(mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round6 Disabled Company",
                                  "licenseNo": "LIC-ROUND6-DISABLED",
                                  "contactPerson": "Carol",
                                  "contactPhone": "13900000004",
                                  "address": "Shangrao City",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Disabled Company Product",
                                  "productCode": "DISABLED-PRODUCT",
                                  "category": "Fruit",
                                  "originPlace": "Shangrao",
                                  "coverImage": "/images/products/orange-batch.svg",
                                  "specification": "10kg/box",
                                  "unit": "box",
                                  "status": "ENABLED"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not available")));
    }
}
