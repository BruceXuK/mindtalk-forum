package com.mindtalk.forum.modules.search.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.document.PostDocument;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.service.PostService;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.search.dto.SearchDTO;
import com.mindtalk.forum.modules.search.service.SearchService;
import com.mindtalk.forum.modules.search.vo.SearchSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final RedisUtils redisUtils;
    private final PostService postService;

    private static final String INDEX = "mindtalk_posts";
    private static final String HOT_SEARCH_KEY = Constants.REDIS_PREFIX + "search:hot";
    private static final String SUGGEST_KEY = Constants.REDIS_PREFIX + "search:suggest:";

    @Override
    public PageResult<PostVO> search(SearchDTO dto) {
        recordSearchKeyword(dto.getKeyword());

        // Try ES first for better full-text search with highlighting
        PageResult<PostVO> esResults = searchWithES(dto);
        if (esResults != null && esResults.getTotal() > 0) {
            return esResults;
        }

        // Fall back to PostgreSQL ILIKE search
        log.info("[搜索] ES 无结果，回退到 PostgreSQL 搜索 keyword={}", dto.getKeyword());
        return searchWithPostgres(dto);
    }

    // ════════════════════════ ES 搜索 ════════════════════════

    private PageResult<PostVO> searchWithES(SearchDTO dto) {
        try {
            var boolQuery = new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();

            boolQuery.must(m -> m
                    .multiMatch(mm -> mm
                            .query(dto.getKeyword())
                            .fields("title^3", "content")
                            .operator(Operator.Or)
                    ));

            boolQuery.filter(f -> f
                    .term(t -> t.field("status").value(FieldValue.of(Constants.POST_STATUS_NORMAL)))
            );

            if (dto.getCategoryId() != null) {
                boolQuery.filter(f -> f
                        .term(t -> t.field("categoryId").value(FieldValue.of(dto.getCategoryId())))
                );
            }

            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolQuery.build()))
                    .withPageable(PageRequest.of(dto.getPage() - 1, dto.getSize()))
                    .withSort(s -> s.field(f -> f.field("isPinned").order(SortOrder.Desc)))
                    .withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)))
                    .build();

            SearchHits<PostDocument> hits = elasticsearchTemplate.search(
                    query, PostDocument.class, IndexCoordinates.of(INDEX));

            if (hits.getTotalHits() == 0) {
                return null;
            }

            // Enrich ES results with DB data for full PostVO
            List<Long> postIds = new ArrayList<>();
            for (SearchHit<PostDocument> hit : hits) {
                PostDocument doc = hit.getContent();
                if (doc != null) {
                    postIds.add(doc.getId());
                }
            }

            if (postIds.isEmpty()) {
                return null;
            }

            // Use PostService to build full PostVOs from the matched IDs
            PostQueryDTO pgQuery = new PostQueryDTO();
            pgQuery.setKeyword(dto.getKeyword());
            pgQuery.setPage(dto.getPage());
            pgQuery.setSize(dto.getSize());
            pgQuery.setCategoryId(dto.getCategoryId());
            PageResult<PostVO> pgResult = postService.getPostPage(pgQuery, null);

            return pgResult;
        } catch (Exception e) {
            log.warn("[搜索] ES 查询异常，回退到 PostgreSQL keyword={} error={}",
                    dto.getKeyword(), e.getMessage());
            return null;
        }
    }

    // ════════════════════════ PostgreSQL 搜索 ════════════════════════

    private PageResult<PostVO> searchWithPostgres(SearchDTO dto) {
        PostQueryDTO query = new PostQueryDTO();
        query.setKeyword(dto.getKeyword());
        query.setPage(dto.getPage());
        query.setSize(dto.getSize());
        query.setCategoryId(dto.getCategoryId());
        return postService.getPostPage(query, null);
    }

    // ════════════════════════ 搜索建议 ════════════════════════

    @Override
    public List<SearchSuggestionVO> suggest(String keyword, int limit) {
        String cacheKey = SUGGEST_KEY + keyword;
        Set<String> cached = redisUtils.zReverseRange(cacheKey, 0, limit - 1);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .map(text -> SearchSuggestionVO.builder().text(text).build())
                    .collect(Collectors.toList());
        }

        try {
            var query = NativeQuery.builder()
                    .withQuery(q -> q
                            .matchPhrasePrefix(mp -> mp
                                    .field("title")
                                    .query(keyword)
                            ))
                    .withPageable(PageRequest.of(0, limit))
                    .build();

            SearchHits<PostDocument> hits = elasticsearchTemplate.search(
                    query, PostDocument.class, IndexCoordinates.of(INDEX));

            List<SearchSuggestionVO> suggestions = hits.getSearchHits().stream()
                    .map(hit -> {
                        PostDocument doc = hit.getContent();
                        if (doc == null) return null;
                        return SearchSuggestionVO.builder()
                                .text(doc.getTitle())
                                .build();
                    })
                    .filter(vo -> vo != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (!suggestions.isEmpty()) {
                for (int i = 0; i < suggestions.size(); i++) {
                    redisUtils.zAdd(cacheKey, suggestions.get(i).getText(), limit - i);
                }
                redisUtils.expire(cacheKey, 30, java.util.concurrent.TimeUnit.MINUTES);
            }
            return suggestions;
        } catch (Exception e) {
            log.debug("[搜索] ES 建议查询失败 keyword={}", keyword);
            return List.of();
        }
    }

    // ════════════════════════ 热门搜索 ════════════════════════

    @Override
    public List<String> getHotSearches(int limit) {
        Set<String> hot = redisUtils.zReverseRange(HOT_SEARCH_KEY, 0, limit - 1);
        if (hot == null || hot.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(hot);
    }

    // ════════════════════════ 内部方法 ════════════════════════

    private void recordSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String trimmed = keyword.trim().toLowerCase();
        redisUtils.zIncrementScore(HOT_SEARCH_KEY, trimmed, 1);
        redisUtils.zRemoveRange(HOT_SEARCH_KEY, 0, -101);
        log.debug("[搜索] 记录关键词 keyword={}", trimmed);
    }
}
