package com.mindtalk.forum.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.forum.modules.user.dto.LoginDTO;
import com.mindtalk.forum.modules.user.dto.RefreshTokenDTO;
import com.mindtalk.forum.modules.user.dto.RegisterDTO;
import com.mindtalk.forum.modules.user.service.UserService;
import com.mindtalk.forum.modules.user.vo.LoginResultVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 接口测试")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("POST /auth/register — 注册成功返回 200")
    void registerSuccess() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setEmail("test@example.com");

        UserVO userVO = UserVO.builder()
                .id(1L).username("testuser").email("test@example.com")
                .createTime(LocalDateTime.now())
                .build();
        when(userService.register(any())).thenReturn(userVO);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /auth/login — 登录成功返回令牌")
    void loginSuccess() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setAccount("testuser");
        dto.setPassword("123456");

        LoginResultVO result = LoginResultVO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(1800L)
                .build();
        when(userService.login(any())).thenReturn(result);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /auth/refresh — 刷新令牌成功")
    void refreshTokenSuccess() throws Exception {
        RefreshTokenDTO refreshDTO = new RefreshTokenDTO();
        refreshDTO.setRefreshToken("old-refresh-token");

        LoginResultVO result = LoginResultVO.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(1800L)
                .build();
        when(userService.refreshToken(any())).thenReturn(result);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }
}
