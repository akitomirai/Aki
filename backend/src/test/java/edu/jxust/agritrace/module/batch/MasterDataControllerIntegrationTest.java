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
    void shouldCreateUpdateAndToggleCompany() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round5 Company",
                                  "licenseNo": "LIC-ROUND5-COMPANY",
                                  "contactPerson": "Alice",
                                  "contactPhone": "13900000001",
                                  "address": "Nanchang City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Round5 Company"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long companyId = created.path("data").path("id").asLong();

        mockMvc.perform(patch("/api/companies/{companyId}", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round5 Company Updated",
                                  "licenseNo": "LIC-ROUND5-COMPANY",
                                  "contactPerson": "Alice Updated",
                                  "contactPhone": "13900000002",
                                  "address": "Ganzhou City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Round5 Company Updated"))
                .andExpect(jsonPath("$.data.contactPerson").value("Alice Updated"));

        mockMvc.perform(post("/api/companies/{companyId}/status", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/companies")
                        .param("keyword", "Round5 Company Updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Round5 Company Updated"));
    }

    @Test
    void shouldCreateUpdateAndFilterProductByCompany() throws Exception {
        MvcResult createCompanyResult = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round5 Product Company",
                                  "licenseNo": "LIC-ROUND5-PRODUCT",
                                  "contactPerson": "Bob",
                                  "contactPhone": "13900000003",
                                  "address": "Yichun City",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        long companyId = objectMapper.readTree(createCompanyResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult createProductResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Round5 Rice",
                                  "productCode": "RICE-ROUND5",
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
                                  "productName": "Round5 Rice Premium",
                                  "productCode": "RICE-ROUND5",
                                  "category": "Grain",
                                  "originPlace": "Yichun High-standard Farm",
                                  "coverImage": "/images/products/rice-batch.svg",
                                  "specification": "20kg/bag",
                                  "unit": "bag",
                                  "status": "ENABLED"
                                }
                                """.formatted(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Round5 Rice Premium"))
                .andExpect(jsonPath("$.data.originPlace").value("Yichun High-standard Farm"));

        mockMvc.perform(get("/api/products")
                        .param("companyId", String.valueOf(companyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyId").value(companyId));
    }

    @Test
    void shouldRejectProductCreationWhenCompanyIsDisabled() throws Exception {
        MvcResult createCompanyResult = mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Round5 Disabled Company",
                                  "licenseNo": "LIC-ROUND5-DISABLED",
                                  "contactPerson": "Carol",
                                  "contactPhone": "13900000004",
                                  "address": "Shangrao City",
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        long companyId = objectMapper.readTree(createCompanyResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

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
                .andExpect(jsonPath("$.message", containsString("disabled")));
    }
}
