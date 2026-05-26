package com.mindtalk.forum.modules.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreatePostDTO;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.service.PostService;
import com.mindtalk.forum.modules.post.vo.PostDetailVO;
import com.mindtalk.forum.modules.post.vo.PostVO;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostController 接口测试")
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    @DisplayName("GET /posts — 分页列表成功")
    void listPostsSuccess() throws Exception {
        PostVO postVO = PostVO.builder()
                .id(1L).title("Test").summary("summary").viewCount(10)
                .likeCount(5).commentCount(3).collectCount(1)
                .isPinned(false).isFeatured(false).status(1)
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();
        PageResult<PostVO> pageResult = PageResult.of(List.of(postVO), 1L, 1, 10);

        when(postService.getPostPage(any(PostQueryDTO.class), isNull()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/posts")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("Test"));
    }

    @Test
    @DisplayName("GET /posts/hot — 热门帖子成功")
    void hotPostsSuccess() throws Exception {
        PostVO postVO = PostVO.builder()
                .id(1L).title("Hot Post").summary("hot").viewCount(100)
                .likeCount(5).commentCount(3).collectCount(1)
                .isPinned(false).isFeatured(false).status(1)
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();

        when(postService.getHotPosts(10)).thenReturn(List.of(postVO));

        mockMvc.perform(get("/posts/hot").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("Hot Post"));
    }

    @Test
    @DisplayName("POST /posts/{id}/view — 记录浏览成功")
    void recordViewSuccess() throws Exception {
        mockMvc.perform(post("/posts/1/view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
