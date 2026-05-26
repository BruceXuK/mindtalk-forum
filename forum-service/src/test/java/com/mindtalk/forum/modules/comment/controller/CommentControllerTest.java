package com.mindtalk.forum.modules.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.exception.GlobalExceptionHandler;
import com.mindtalk.forum.modules.comment.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController 接口测试")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController commentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /comments — 分页查询评论列表")
    void listComments() throws Exception {
        mockMvc.perform(get("/comments")
                        .param("postId", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /comments/{id} — 删除评论需登录，返回 500")
    void deleteCommentRequiresAuth() throws Exception {
        mockMvc.perform(delete("/comments/1"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500));
    }
}
