package com.mindtalk.forum.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.post.dto.CreateTagDTO;
import com.mindtalk.forum.modules.post.entity.PostTag;
import com.mindtalk.forum.modules.post.entity.Tag;
import com.mindtalk.forum.modules.post.mapper.PostTagMapper;
import com.mindtalk.forum.modules.post.mapper.TagMapper;
import com.mindtalk.forum.modules.post.service.TagService;
import com.mindtalk.forum.modules.post.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 标签服务实现 — 含防雪崩/穿透/击穿的缓存策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = Constants.REDIS_PREFIX + "tag:list";
    private static final String NULL_MARKER = "[]";
    /** base TTL seconds (5 min) */
    private static final long BASE_TTL_SEC = 300;
    /** max jitter seconds (±30s = ±10%) */
    private static final long MAX_JITTER_SEC = 30;
    /** null cache TTL (1 min for empty results, anti-penetration) */
    private static final long NULL_TTL_SEC = 60;
    /** mutex lock TTL (anti-breakdown) */
    private static final long LOCK_TTL_SEC = 5;
    private static final String LOCK_SUFFIX = ":lock";

    // ── Cache helpers ──

    /** TTL with random jitter to prevent avalanche */
    private long ttlWithJitter() {
        long jitter = ThreadLocalRandom.current().nextLong(-MAX_JITTER_SEC, MAX_JITTER_SEC + 1);
        return Math.max(BASE_TTL_SEC + jitter, 60); // minimum 1 min
    }

    /** Rebuild list cache: DB query + serialize + set with jittered TTL */
    private List<TagVO> rebuildAndCache() {
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getStatus, 1)
                        .orderByDesc(Tag::getPostCount));

        List<TagVO> vos = tags.stream().map(t -> TagVO.builder()
                .id(t.getId()).name(t.getName()).postCount(t.getPostCount())
                .build()).toList();

        try {
            String value = vos.isEmpty() ? NULL_MARKER : objectMapper.writeValueAsString(vos);
            long ttl = vos.isEmpty() ? NULL_TTL_SEC : ttlWithJitter();
            redisUtils.set(CACHE_KEY, value, ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 标签列表序列化失败");
            // still return data, just don't cache
        }
        return vos;
    }

    /**
     * 带防击穿（mutex）的缓存读取。
     * 缓存未命中时竞争重建锁，拿到锁的线程负责查库重建，未拿到的短暂等待后重试缓存。
     */
    private List<TagVO> listWithBreakdownGuard() {
        // 1. 读缓存
        String cached = redisUtils.get(CACHE_KEY);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) return Collections.emptyList();
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TagVO>>() {});
            } catch (JsonProcessingException e) {
                log.debug("[缓存] 标签列表反序列化失败");
            }
        }

        // 2. 缓存未命中 → 竞争重建锁（防击穿）
        String lockKey = CACHE_KEY + LOCK_SUFFIX;
        Boolean locked = redisUtils.setIfAbsent(lockKey, "1", LOCK_TTL_SEC, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                // Double-check cache (another thread may have rebuilt)
                cached = redisUtils.get(CACHE_KEY);
                if (cached != null) {
                    if (NULL_MARKER.equals(cached)) return Collections.emptyList();
                    return objectMapper.readValue(cached, new TypeReference<List<TagVO>>() {});
                }
                return rebuildAndCache();
            } catch (JsonProcessingException e) {
                log.warn("[缓存] 反序列化异常，直接查库");
            } finally {
                redisUtils.delete(lockKey);
            }
        }

        // 3. 没拿到锁 → 短暂等待后重试缓存
        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        return listWithBreakdownGuard();
    }

    // ── Public API ──

    @Override
    public List<TagVO> search(String q) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getStatus, 1)
                .apply("name ILIKE {0}", "%" + q + "%")
                .orderByDesc(Tag::getPostCount)
                .last("LIMIT 10");
        return tagMapper.selectList(wrapper).stream()
                .map(t -> TagVO.builder()
                        .id(t.getId()).name(t.getName()).postCount(t.getPostCount())
                        .build()).toList();
    }

    @Override
    @Transactional
    public TagVO getOrCreate(String name, String description) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("LOWER(name) = LOWER({0})", name);
        Tag existing = tagMapper.selectOne(wrapper);
        if (existing != null) {
            return TagVO.builder()
                    .id(existing.getId()).name(existing.getName())
                    .postCount(existing.getPostCount()).status(existing.getStatus())
                    .build();
        }

        Tag tag = Tag.builder()
                .name(name)
                .description(description)
                .postCount(0)
                .status(1)
                .build();
        tagMapper.insert(tag);

        invalidateCache();
        log.info("[标签] 用户自动创建 id={} name={}", tag.getId(), tag.getName());
        return TagVO.builder()
                .id(tag.getId()).name(tag.getName())
                .postCount(0).status(1).build();
    }

    @Override
    public List<TagVO> list() {
        return listWithBreakdownGuard();
    }

    @Override
    @Transactional
    public TagVO create(CreateTagDTO dto) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, dto.getName());
        if (tagMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("标签名称已存在");
        }

        Tag tag = Tag.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .postCount(0)
                .status(1)
                .build();
        tagMapper.insert(tag);

        invalidateCache();
        log.info("[标签] 创建成功 id={} name={}", tag.getId(), tag.getName());
        return TagVO.builder().id(tag.getId()).name(tag.getName()).postCount(0).status(tag.getStatus()).build();
    }

    @Override
    @Transactional
    public TagVO update(Long id, CreateTagDTO dto) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }

        if (dto.getName() != null) tag.setName(dto.getName());
        if (dto.getDescription() != null) tag.setDescription(dto.getDescription());
        tagMapper.updateById(tag);

        invalidateCache();
        log.info("[标签] 编辑成功 id={}", id);
        return TagVO.builder().id(tag.getId()).name(tag.getName())
                .postCount(tag.getPostCount()).status(tag.getStatus()).build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }
        LambdaQueryWrapper<PostTag> ptWrapper = new LambdaQueryWrapper<>();
        ptWrapper.eq(PostTag::getTagId, id);
        postTagMapper.delete(ptWrapper);
        tagMapper.deleteById(id);

        invalidateCache();
        log.info("[标签] 删除成功 id={}", id);
    }

    @Override
    @Transactional
    public void merge(List<Long> sourceIds, Long targetId) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            throw BusinessException.badRequest("请选择要合并的标签");
        }
        if (sourceIds.contains(targetId)) {
            throw BusinessException.badRequest("目标标签不能包含在源标签中");
        }
        Tag targetTag = tagMapper.selectById(targetId);
        if (targetTag == null) {
            throw BusinessException.notFound("目标标签不存在");
        }

        LambdaQueryWrapper<PostTag> ptWrapper = new LambdaQueryWrapper<>();
        ptWrapper.in(PostTag::getTagId, sourceIds);
        List<PostTag> sourceAssocs = postTagMapper.selectList(ptWrapper);

        LambdaQueryWrapper<PostTag> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(PostTag::getTagId, targetId);
        List<PostTag> targetAssocs = postTagMapper.selectList(targetWrapper);
        java.util.Set<Long> existingPostIds = targetAssocs.stream()
                .map(PostTag::getPostId)
                .collect(java.util.stream.Collectors.toSet());

        for (PostTag assoc : sourceAssocs) {
            if (!existingPostIds.contains(assoc.getPostId())) {
                assoc.setTagId(targetId);
                postTagMapper.updateById(assoc);
            } else {
                postTagMapper.deleteById(assoc.getId());
            }
        }

        tagMapper.deleteBatchIds(sourceIds);

        long totalCount = postTagMapper.selectCount(
                new LambdaQueryWrapper<PostTag>().eq(PostTag::getTagId, targetId));
        targetTag.setPostCount((int) totalCount);
        tagMapper.updateById(targetTag);

        invalidateCache();
        log.info("[标签] 合并成功 sourceIds={} -> targetId={}", sourceIds, targetId);
    }

    @Override
    public List<TagVO> listAll() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getPostCount);
        return tagMapper.selectList(wrapper).stream()
                .map(t -> TagVO.builder()
                        .id(t.getId()).name(t.getName()).description(t.getDescription())
                        .postCount(t.getPostCount()).status(t.getStatus())
                        .build()).toList();
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }
        tag.setStatus(tag.getStatus() == 1 ? 0 : 1);
        tagMapper.updateById(tag);

        invalidateCache();
        log.info("[标签] 状态切换 id={} status={}", id, tag.getStatus());
    }

    // ── Internal ──

    /** Invalidate the list cache after any write operation */
    private void invalidateCache() {
        redisUtils.delete(CACHE_KEY);
    }
}
