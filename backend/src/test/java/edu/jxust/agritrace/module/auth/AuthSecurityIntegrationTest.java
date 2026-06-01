package edu.jxust.agritrace.module.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void shouldRejectTokenAfterUserIsDisabled() throws Exception {
        String token = loginToken("platform", "123456");
        SysUserPO user = sysUserMapper.selectById(1L);
        Integer originalStatus = user.getStatus();

        try {
            user.setStatus(0);
            sysUserMapper.updateById(user);

            mockMvc.perform(get("/api/batches")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        } finally {
            user.setStatus(originalStatus);
            sysUserMapper.updateById(user);
        }
    }

    @Test
    void shouldUseLatestRoleForExistingToken() throws Exception {
        String token = loginToken("platform", "123456");
        SysUserPO user = sysUserMapper.selectById(1L);
        String originalRole = user.getRoleCode();

        try {
            user.setRoleCode("REGULATOR");
            sysUserMapper.updateById(user);

            mockMvc.perform(get("/api/users")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("你没有用户管理权限。"));
        } finally {
            user.setRoleCode(originalRole);
            sysUserMapper.updateById(user);
        }
    }

    @Test
    void shouldKeepSelfRegistrationClosed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void platformAdminShouldCreateManagedUserWithPhoneAndSearchByPhone() throws Exception {
        String token = loginToken("platform", "123456");

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "operator_phone_it",
                                  "password": "123456",
                                  "realName": "Phone Operator",
                                  "phone": "13900001234",
                                  "roleCode": "OPERATOR",
                                  "companyId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("operator_phone_it"))
                .andExpect(jsonPath("$.data.phone").value("13900001234"))
                .andExpect(jsonPath("$.data.needChangePassword").value(true));

        mockMvc.perform(get("/api/users")
                        .queryParam("keyword", "13900001234")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("operator_phone_it"))
                .andExpect(jsonPath("$.data[0].phone").value("13900001234"));
    }

    @Test
    void enterpriseAdminShouldOnlyOpenAccountsInsideEnterpriseScope() throws Exception {
        String token = loginToken("enterprise_admin", "123456");

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "enterprise_operator_it",
                                  "password": "123456",
                                  "realName": "Enterprise Operator",
                                  "phone": "13900005678",
                                  "roleCode": "OPERATOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("OPERATOR"))
                .andExpect(jsonPath("$.data.companyId").value(1))
                .andExpect(jsonPath("$.data.phone").value("13900005678"));

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "regulator_denied_it",
                                  "password": "123456",
                                  "realName": "Regulator Denied",
                                  "roleCode": "REGULATOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("企业管理员只能创建和管理本企业管理员、操作员。"));
    }

    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "loginChannel": "ADMIN"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("token").asText();
    }
}
