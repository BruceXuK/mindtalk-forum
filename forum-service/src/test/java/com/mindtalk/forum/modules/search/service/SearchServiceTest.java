package com.mindtalk.forum.modules.search.service;

import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.service.PostService;
import com.mindtalk.forum.modules.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService 单元测试")
class SearchServiceTest {

    @Mock
    private ElasticsearchTemplate elasticsearchTemplate;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private PostService postService;

    @InjectMocks
    private SearchServiceImpl searchService;

    @Nested
    @DisplayName("热门搜索")
    class HotSearchTests {

        @Test
        @DisplayName("返回热门搜索列表")
        void shouldReturnHotSearches() {
            when(redisUtils.zReverseRange(anyString(), eq(0L), eq(9L)))
                    .thenReturn(Set.of("spring", "java", "docker"));

            List<String> hot = searchService.getHotSearches(10);

            assertThat(hot).hasSize(3);
            assertThat(hot).contains("spring");
        }

        @Test
        @DisplayName("无热门搜索返回空列表")
        void shouldReturnEmptyWhenNoHotSearches() {
            when(redisUtils.zReverseRange(anyString(), eq(0L), eq(9L)))
                    .thenReturn(null);

            List<String> hot = searchService.getHotSearches(10);

            assertThat(hot).isEmpty();
        }
    }
}
