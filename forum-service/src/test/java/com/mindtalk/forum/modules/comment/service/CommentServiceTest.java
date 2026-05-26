package com.mindtalk.forum.modules.comment.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.component.RocketMQProducer;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.comment.dto.CreateCommentDTO;
import com.mindtalk.forum.modules.comment.entity.Comment;
import com.mindtalk.forum.modules.comment.entity.Like;
import com.mindtalk.forum.modules.comment.mapper.CommentMapper;
import com.mindtalk.forum.modules.comment.mapper.LikeMapper;
import com.mindtalk.forum.modules.comment.service.impl.CommentServiceImpl;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 单元测试")
class CommentServiceTest {

    @Mock private CommentMapper commentMapper;
    @Mock private LikeMapper likeMapper;
    @Mock private PostMapper postMapper;
    @Mock private UserMapper userMapper;
    @Mock private RedisUtils redisUtils;
    @Mock private RocketMQProducer rocketMQProducer;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Post testPost;
    private User testUser;

    @BeforeEach
    void setUp() {
        testPost = Post.builder().id(1L).title("Test").authorId(2L).build();
        testUser = User.builder().id(1L).username("user1").nickname("User1")
                .avatarUrl("http://a").status(1).build();
    }

    @Nested
    @DisplayName("发表评论")
    class CreateCommentTests {

        @Test
        @DisplayName("发表一级评论成功")
        void shouldCreateFirstLevelComment() {
            CreateCommentDTO dto = new CreateCommentDTO();
            dto.setPostId(1L);
            dto.setContent("Great post!");

            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(commentMapper.insert(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(10L);
                return 1;
            });
            when(postMapper.update(any(LambdaUpdateWrapper.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(likeMapper.exists(anyLong(), anyString(), anyLong())).thenReturn(0);

            var result = commentService.createComment(1L, dto);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("Great post!");
            verify(rocketMQProducer, atLeastOnce()).sendAsync(anyString(), anyMap());
        }

        @Test
        @DisplayName("帖子不存在应抛异常")
        void shouldThrowWhenPostNotFound() {
            when(postMapper.selectById(999L)).thenReturn(null);

            CreateCommentDTO dto = new CreateCommentDTO();
            dto.setPostId(999L);
            dto.setContent("test");

            assertThatThrownBy(() -> commentService.createComment(1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("帖子不存在");
        }
    }

    @Nested
    @DisplayName("点赞评论")
    class LikeCommentTests {

        @Test
        @DisplayName("点赞成功")
        void shouldLikeComment() {
            Comment comment = Comment.builder().id(1L).postId(1L).userId(2L)
                    .content("Nice").likeCount(0).status(1).build();

            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(likeMapper.exists(1L, "COMMENT", 1L)).thenReturn(0);
            when(likeMapper.insert(any(Like.class))).thenReturn(1);
            when(commentMapper.update(any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertThatCode(() -> commentService.likeComment(1L, 1L))
                    .doesNotThrowAnyException();
            verify(rocketMQProducer).sendAsync(eq("like-event"), anyMap());
        }

        @Test
        @DisplayName("取消点赞成功")
        void shouldUnlikeComment() {
            Comment comment = Comment.builder().id(1L).postId(1L).userId(2L)
                    .content("Nice").likeCount(1).status(1).build();
            Like like = Like.builder().id(100L).userId(1L).targetType("COMMENT").targetId(1L).build();

            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(likeMapper.exists(1L, "COMMENT", 1L)).thenReturn(1);
            when(likeMapper.selectOne(any())).thenReturn(like);
            when(likeMapper.deleteById(100L)).thenReturn(1);
            when(commentMapper.update(any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertThatCode(() -> commentService.likeComment(1L, 1L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("删除评论")
    class DeleteCommentTests {

        @Test
        @DisplayName("删除自己的评论成功")
        void shouldDeleteOwnComment() {
            Comment comment = Comment.builder().id(1L).postId(1L).userId(1L)
                    .content("My comment").status(1).build();

            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentMapper.deleteById(1L)).thenReturn(1);
            when(postMapper.update(any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertThatCode(() -> commentService.deleteComment(1L, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("删除他人评论应抛异常")
        void shouldThrowWhenNotOwner() {
            Comment comment = Comment.builder().id(1L).postId(1L).userId(2L)
                    .content("Other").status(1).build();

            when(commentMapper.selectById(1L)).thenReturn(comment);

            assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只能删除自己的评论");
        }
    }
}
