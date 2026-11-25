package com.ourgalaxy.system;

import com.ourgalaxy.common.config.SecurityConfig;
import com.ourgalaxy.common.security.JwtAuthenticationFilter;
import com.ourgalaxy.common.security.JwtService;
import com.ourgalaxy.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SystemControllerTest {

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthShouldBePublicAndReturnApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("our-galaxy-backend"))
                .andExpect(jsonPath("$.data.scope").value("APPLICATION"));
    }

    @Test
    void unknownApiShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/private"))
                .andExpect(status().isUnauthorized());
    }
}
