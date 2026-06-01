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
