package com.mindtalk.forum.modules.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.component.RocketMQProducer;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.dto.CreatePostDTO;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.dto.UpdatePostDTO;
import com.mindtalk.forum.modules.post.entity.Category;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.CategoryMapper;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.mapper.PostTagMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.post.service.impl.PostServiceImpl;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 单元测试")
class PostServiceTest {

    @Mock private PostMapper postMapper;
    @Mock private PostTagMapper postTagMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TagMapper tagMapper;
    @Mock private UserMapper userMapper;
    @Mock private RedisUtils redisUtils;
    @Mock private RocketMQProducer rocketMQProducer;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private PostServiceImpl postService;

    private Post testPost;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("author").nickname("Author")
                .avatarUrl("http://avatar").status(1).build();

        testPost = Post.builder()
                .id(1L).title("Test Post").content("# Hello").contentText("Hello")
                .authorId(1L).categoryId(1L).isPinned(false).isFeatured(false)
                .viewCount(10).likeCount(5).commentCount(3).collectCount(2)
                .status(1).createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("发帖")
    class CreatePostTests {

        @Test
        @DisplayName("正常发帖成功")
        void shouldCreatePostSuccessfully() {
            CreatePostDTO dto = new CreatePostDTO();
            dto.setTitle("New Post");
            dto.setContent("# Content");
            dto.setContentText("Content");
            dto.setCategoryId(1L);
            dto.setTagIds(List.of(1L, 2L));

            when(postMapper.insert(any(Post.class))).thenAnswer(inv -> {
                Post p = inv.getArgument(0);
                p.setId(10L);
                return 1;
            });
            when(postMapper.selectById(10L)).thenAnswer(inv -> {
                Post p = Post.builder()
                        .id(10L).title("New Post").content("# Content").contentText("Content")
                        .authorId(1L).categoryId(1L).isPinned(false).isFeatured(false)
                        .viewCount(0).likeCount(0).commentCount(0).collectCount(0)
                        .status(1).createTime(LocalDateTime.now()).updateTime(LocalDateTime.now())
                        .build();
                return p;
            });
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(categoryMapper.selectById(1L)).thenReturn(
                    Category.builder().id(1L).name("Tech").build());
            when(tagMapper.selectByPostId(10L)).thenReturn(List.of(
                    Tag.builder().id(1L).name("Java").postCount(10).build(),
                    Tag.builder().id(2L).name("Spring").postCount(5).build()
            ));
            when(postTagMapper.batchInsert(anyList())).thenReturn(1);
            doNothing().when(rocketMQProducer).sendAsync(anyString(), anyMap());

            var result = postService.createPost(1L, dto);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("New Post");
            verify(postTagMapper).batchInsert(anyList());
            verify(rocketMQProducer, atLeastOnce()).sendAsync(anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("编辑帖子")
    class UpdatePostTests {

        @Test
        @DisplayName("编辑自己的帖子成功")
        void shouldUpdateOwnPost() {
            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(postMapper.updateById(any(Post.class))).thenReturn(1);
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(categoryMapper.selectById(1L)).thenReturn(
                    Category.builder().id(1L).name("Tech").build());
            when(tagMapper.selectByPostId(1L)).thenReturn(Collections.emptyList());

            UpdatePostDTO dto = new UpdatePostDTO();
            dto.setTitle("Updated Title");

            var result = postService.updatePost(1L, 1L, dto);

            assertThat(result.getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("编辑他人帖子应抛异常")
        void shouldThrowWhenNotOwner() {
            when(postMapper.selectById(1L)).thenReturn(testPost);

            UpdatePostDTO dto = new UpdatePostDTO();
            dto.setTitle("Hacked");

            assertThatThrownBy(() -> postService.updatePost(2L, 1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只能编辑自己的帖子");
        }
    }

    @Nested
    @DisplayName("删除帖子")
    class DeletePostTests {

        @Test
        @DisplayName("作者删除自己的帖子成功")
        void shouldDeleteOwnPost() {
            when(postMapper.selectById(1L)).thenReturn(testPost);
            when(postMapper.deleteById(1L)).thenReturn(1);
            when(postTagMapper.deleteByPostId(1L)).thenReturn(1);

            assertThatCode(() -> postService.deletePost(1L, 1L))
                    .doesNotThrowAnyException();
            verify(postMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("分页查询")
    class PageQueryTests {

        @Test
        @DisplayName("分页返回正确结构")
        void shouldReturnPageResult() {
            PostQueryDTO query = new PostQueryDTO();
            query.setPage(1);
            query.setSize(10);

            Page<Post> page = new Page<>(1, 10);
            // Use the mockPage directly since we can't easily mock MyBatis-Plus Page internals
            when(postMapper.selectPageWithAuthor(any(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                    .thenAnswer(inv -> {
                        Page<Post> p = inv.getArgument(0);
                        p.setRecords(List.of(testPost));
                        p.setTotal(1);
                        return p;
                    });
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(categoryMapper.selectById(1L)).thenReturn(
                    Category.builder().id(1L).name("Tech").build());
            when(tagMapper.selectByPostId(1L)).thenReturn(Collections.emptyList());

            var result = postService.getPostPage(query, null);

            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("帖子详情")
    class DetailTests {

        @Test
        @DisplayName("帖子不存在应抛异常")
        void shouldThrowWhenPostNotFound() {
            when(postMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> postService.getPostDetail(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("帖子不存在");
        }
    }

    @Nested
    @DisplayName("热门帖子")
    class HotPostsTests {

        @Test
        @DisplayName("查询热门帖子成功")
        void shouldReturnHotPosts() {
            when(redisUtils.get(anyString())).thenReturn(null);
            when(postMapper.selectHotPosts(10)).thenReturn(List.of(testPost));
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(categoryMapper.selectById(1L)).thenReturn(
                    Category.builder().id(1L).name("Tech").build());
            when(tagMapper.selectByPostId(1L)).thenReturn(Collections.emptyList());

            var result = postService.getHotPosts(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Test Post");
        }
    }
}
