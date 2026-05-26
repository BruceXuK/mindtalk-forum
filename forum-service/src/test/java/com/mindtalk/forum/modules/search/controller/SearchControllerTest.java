package com.mindtalk.forum.modules.search.controller;

import com.mindtalk.forum.modules.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController 接口测试")
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    @Test
    @DisplayName("GET /search — 关键词搜索成功")
    void searchSuccess() throws Exception {
        mockMvc.perform(get("/search")
                        .param("keyword", "spring")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /search — 缺少关键词返回 400")
    void searchMissingKeyword() throws Exception {
        mockMvc.perform(get("/search")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /search/suggest — 搜索建议成功")
    void suggestSuccess() throws Exception {
        when(searchService.suggest(anyString(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/search/suggest")
                        .param("keyword", "spr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /search/hot — 热门搜索成功")
    void hotSearchSuccess() throws Exception {
        when(searchService.getHotSearches(anyInt())).thenReturn(List.of("java", "spring"));

        mockMvc.perform(get("/search/hot").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("java"));
    }
}
