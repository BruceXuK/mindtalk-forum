package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.StatsOverviewVO;
import com.mindtalk.forum.modules.admin.vo.StatsTrendVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatsController")
class AdminStatsControllerTest {

    @Mock private AdminService adminService;
    @InjectMocks private AdminStatsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("GET /admin/stats/overview")
    class Overview {

        @Test
        @DisplayName("返回统计概览")
        void shouldReturnOverview() throws Exception {
            StatsOverviewVO vo = StatsOverviewVO.builder()
                    .totalUsers(100L).totalPosts(50L).totalComments(200L)
                    .totalReports(10L).todayNewUsers(5L).todayNewPosts(3L)
                    .todayNewComments(10L).pendingReports(2L).build();
            when(adminService.getStatsOverview()).thenReturn(vo);

            mockMvc.perform(get("/admin/stats/overview"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.totalUsers").value(100))
                    .andExpect(jsonPath("$.data.totalPosts").value(50));
        }
    }

    @Nested
    @DisplayName("GET /admin/stats/trends")
    class Trends {

        @Test
        @DisplayName("返回趋势统计")
        void shouldReturnTrends() throws Exception {
            StatsTrendVO trend = StatsTrendVO.builder()
                    .date("2026-05-21").newUsers(5L).newPosts(3L).newComments(10L).build();
            when(adminService.getStatsTrends(7)).thenReturn(List.of(trend));

            mockMvc.perform(get("/admin/stats/trends").param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].date").value("2026-05-21"))
                    .andExpect(jsonPath("$.data[0].newUsers").value(5));
        }
    }
}
