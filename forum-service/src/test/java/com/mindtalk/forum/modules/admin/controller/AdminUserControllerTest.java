package com.mindtalk.forum.modules.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.admin.dto.RoleAssignDTO;
import com.mindtalk.forum.modules.admin.dto.UserManageDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.AdminUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController")
class AdminUserControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminUserController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("GET /admin/users")
    class ListUsers {

        @Test
        @DisplayName("返回用户分页列表")
        void shouldReturnUserPage() throws Exception {
            AdminUserVO vo = AdminUserVO.builder()
                    .id(1L).username("testuser").email("test@test.com")
                    .nickname("Test").status(1).roles(List.of("USER"))
                    .createTime(LocalDateTime.now()).build();
            when(adminService.getUserPage(any(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.of(List.of(vo), 1L, 1, 20));

            mockMvc.perform(get("/admin/users")
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records[0].username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("GET /admin/users/{id}")
    class GetUserDetail {

        @Test
        @DisplayName("返回用户详情")
        void shouldReturnUserDetail() throws Exception {
            AdminUserVO vo = AdminUserVO.builder()
                    .id(1L).username("testuser").email("test@test.com")
                    .nickname("Test").status(1).roles(List.of("USER"))
                    .createTime(LocalDateTime.now()).build();
            when(adminService.getUserDetail(1L)).thenReturn(vo);

            mockMvc.perform(get("/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("PUT /admin/users/{id}/status")
    class UpdateUserStatus {

        @Test
        @DisplayName("修改用户状态成功")
        void shouldUpdateStatus() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("1", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            UserManageDTO dto = new UserManageDTO();
            dto.setStatus(0);

            mockMvc.perform(put("/admin/users/2/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
