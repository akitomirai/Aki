package edu.jxust.agritrace.module.batch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.module.log.mapper.OperationAuditLogMapper;
import edu.jxust.agritrace.module.log.mapper.po.OperationAuditLogPO;
import edu.jxust.agritrace.support.AuthenticatedIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MasterDataControllerIntegrationTest extends AuthenticatedIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationAuditLogMapper operationAuditLogMapper;

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
                .andExpect(jsonPath("$.message", containsString("已关联")));
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
                .andExpect(jsonPath("$.message", containsString("已关联")));
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
                .andExpect(jsonPath("$.message", containsString("不可用")));
    }

    @Test
    void shouldAllowEnterpriseAdminToViewAndUpdateOwnCompanyOnly() throws Exception {
        authenticateAs(ENTERPRISE_ADMIN_SESSION);

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1));

        mockMvc.perform(get("/api/companies/{companyId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(patch("/api/companies/{companyId}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Demo Orchard Company",
                                  "licenseNo": "LIC-DEMO-ORCHARD",
                                  "contactPerson": "Enterprise Scope Contact",
                                  "contactPhone": "13912345678",
                                  "address": "Ganzhou Enterprise Scope Road",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.contactPerson").value("Enterprise Scope Contact"))
                .andExpect(jsonPath("$.data.contactPhone").value("13912345678"))
                .andExpect(jsonPath("$.data.address").value("Ganzhou Enterprise Scope Road"));
    }

    @Test
    void shouldRejectEnterpriseAdminCrossCompanyAccessAndCreationForCompanies() throws Exception {
        long foreignCompanyId = createCompanyAsPlatform("Scope Foreign Company", "LIC-SCOPE-FOREIGN-COMPANY");
        long beforeForeignAccess = countDeniedLogs("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看和维护本企业资料。");
        long beforeCreate = countDeniedLogs("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "企业管理员不能新建企业资料。");

        authenticateAs(ENTERPRISE_ADMIN_SESSION);

        mockMvc.perform(get("/api/companies/{companyId}", foreignCompanyId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能查看和维护本企业资料。"));

        mockMvc.perform(patch("/api/companies/{companyId}", foreignCompanyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Scope Foreign Company Updated",
                                  "licenseNo": "LIC-SCOPE-FOREIGN-COMPANY",
                                  "contactPerson": "Mallory",
                                  "contactPhone": "13955550001",
                                  "address": "Forbidden Address",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能查看和维护本企业资料。"));

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Enterprise Admin Forbidden Company",
                                  "licenseNo": "LIC-ENT-FORBID-COMPANY",
                                  "contactPerson": "Mallory",
                                  "contactPhone": "13955550002",
                                  "address": "Forbidden Address",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("企业管理员不能新建企业资料。"));

        assertThat(countDeniedLogs("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看和维护本企业资料。"))
                .isEqualTo(beforeForeignAccess + 2);
        assertThat(countDeniedLogs("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "企业管理员不能新建企业资料。"))
                .isEqualTo(beforeCreate + 1);

        OperationAuditLogPO foreignDeniedLog = findLatestDeniedLog("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看和维护本企业资料。");
        assertThat(foreignDeniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(foreignDeniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(foreignDeniedLog.getTargetType()).isEqualTo("COMPANY");
        assertThat(foreignDeniedLog.getTargetId()).isEqualTo(foreignCompanyId);
        assertThat(foreignDeniedLog.getResult()).isEqualTo("FAILED");

        OperationAuditLogPO createDeniedLog = findLatestDeniedLog("COMPANY_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "企业管理员不能新建企业资料。");
        assertThat(createDeniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(createDeniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(createDeniedLog.getTargetType()).isEqualTo("COMPANY");
        assertThat(createDeniedLog.getResult()).isEqualTo("FAILED");
    }

    @Test
    void shouldRestrictEnterpriseAdminProductsToOwnCompanyAndRecordDeniedLogs() throws Exception {
        long foreignCompanyId = createCompanyAsPlatform("Scope Foreign Product Company", "LIC-SCOPE-FOREIGN-PRODUCT");
        long foreignProductId = createProductAsPlatform(
                foreignCompanyId,
                "Scope Foreign Product",
                "SCOPE-FOREIGN-PRODUCT"
        );

        long beforeFilterDenied = countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业产品。");
        long beforeCreateDenied = countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能创建本企业产品，不能挂到其他企业。");
        long beforeReassignDenied = countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能维护本企业产品，不能改挂到其他企业。");

        authenticateAs(ENTERPRISE_ADMIN_SESSION);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].companyId", everyItem(is(1))));

        mockMvc.perform(get("/api/products")
                        .param("companyId", String.valueOf(foreignCompanyId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能查看本企业产品。"));

        mockMvc.perform(get("/api/products/{productId}", foreignProductId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能查看本企业产品。"));

        MvcResult ownCreateResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "productName": "Enterprise Own Product",
                                  "productCode": "ENT-OWN-PRODUCT",
                                  "category": "Fruit",
                                  "originPlace": "Ganzhou",
                                  "coverImage": "/images/products/orange-batch.svg",
                                  "specification": "8kg/box",
                                  "unit": "box",
                                  "status": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyId").value(1))
                .andExpect(jsonPath("$.data.productName").value("Enterprise Own Product"))
                .andReturn();

        long ownProductId = objectMapper.readTree(ownCreateResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Enterprise Forbidden Product",
                                  "productCode": "ENT-FORBIDDEN-PRODUCT",
                                  "category": "Fruit",
                                  "originPlace": "Other Company",
                                  "coverImage": "/images/products/orange-batch.svg",
                                  "specification": "9kg/box",
                                  "unit": "box",
                                  "status": "ENABLED"
                                }
                                """.formatted(foreignCompanyId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能创建本企业产品，不能挂到其他企业。"));

        mockMvc.perform(patch("/api/products/{productId}", ownProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": %d,
                                  "productName": "Enterprise Own Product Updated",
                                  "productCode": "ENT-OWN-PRODUCT",
                                  "category": "Fruit",
                                  "originPlace": "Other Company",
                                  "coverImage": "/images/products/orange-batch.svg",
                                  "specification": "8kg/box",
                                  "unit": "box",
                                  "status": "ENABLED"
                                }
                                """.formatted(foreignCompanyId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你只能维护本企业产品，不能改挂到其他企业。"));

        assertThat(countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业产品。"))
                .isEqualTo(beforeFilterDenied + 2);
        assertThat(countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能创建本企业产品，不能挂到其他企业。"))
                .isEqualTo(beforeCreateDenied + 1);
        assertThat(countDeniedLogs("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能维护本企业产品，不能改挂到其他企业。"))
                .isEqualTo(beforeReassignDenied + 1);

        OperationAuditLogPO filterDeniedLog = findLatestDeniedLog("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能查看本企业产品。");
        assertThat(filterDeniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(filterDeniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(filterDeniedLog.getTargetType()).isEqualTo("PRODUCT");
        assertThat(filterDeniedLog.getResult()).isEqualTo("FAILED");

        OperationAuditLogPO createDeniedLog = findLatestDeniedLog("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能创建本企业产品，不能挂到其他企业。");
        assertThat(createDeniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(createDeniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(createDeniedLog.getTargetType()).isEqualTo("PRODUCT");
        assertThat(createDeniedLog.getResult()).isEqualTo("FAILED");

        OperationAuditLogPO reassignDeniedLog = findLatestDeniedLog("PRODUCT_ACCESS_DENIED", ENTERPRISE_ADMIN_SESSION.userId(), "你只能维护本企业产品，不能改挂到其他企业。");
        assertThat(reassignDeniedLog.getCompanyId()).isEqualTo(1L);
        assertThat(reassignDeniedLog.getRoleCode()).isEqualTo("ENTERPRISE_ADMIN");
        assertThat(reassignDeniedLog.getTargetType()).isEqualTo("PRODUCT");
        assertThat(reassignDeniedLog.getResult()).isEqualTo("FAILED");
    }

    private long createCompanyAsPlatform(String name, String licenseNo) throws Exception {
        authenticateAs(PLATFORM_ADMIN_SESSION);
        String payload = """
                {
                  "name": "%s",
                  "licenseNo": "%s",
                  "contactPerson": "Scope Seeder",
                  "contactPhone": "13966660001",
                  "address": "Scope Seed Address",
                  "status": "ENABLED"
                }
                """.formatted(name, licenseNo);
        return objectMapper.readTree(mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long createProductAsPlatform(long companyId, String productName, String productCode) throws Exception {
        authenticateAs(PLATFORM_ADMIN_SESSION);
        String payload = """
                {
                  "companyId": %d,
                  "productName": "%s",
                  "productCode": "%s",
                  "category": "Fruit",
                  "originPlace": "Scope Seed Origin",
                  "coverImage": "/images/products/orange-batch.svg",
                  "specification": "10kg/box",
                  "unit": "box",
                  "status": "ENABLED"
                }
                """.formatted(companyId, productName, productCode);
        return objectMapper.readTree(mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private long countDeniedLogs(String actionType, Long operatorUserId, String summary) {
        return operationAuditLogMapper.selectCount(new LambdaQueryWrapper<OperationAuditLogPO>()
                .eq(OperationAuditLogPO::getActionType, actionType)
                .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                .eq(OperationAuditLogPO::getSummary, summary));
    }

    private OperationAuditLogPO findLatestDeniedLog(String actionType, Long operatorUserId, String summary) {
        return operationAuditLogMapper.selectList(new LambdaQueryWrapper<OperationAuditLogPO>()
                        .eq(OperationAuditLogPO::getActionType, actionType)
                        .eq(OperationAuditLogPO::getOperatorUserId, operatorUserId)
                        .eq(OperationAuditLogPO::getSummary, summary)
                        .orderByDesc(OperationAuditLogPO::getId))
                .stream()
                .findFirst()
                .orElseThrow();
    }
}
