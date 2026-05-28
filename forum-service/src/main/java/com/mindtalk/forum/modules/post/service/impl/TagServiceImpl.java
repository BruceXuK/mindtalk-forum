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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 标签服务实现
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
    private static final long CACHE_TTL = 30;

    @Override
    public List<TagVO> list() {
        String cached = redisUtils.get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TagVO>>() {});
            } catch (JsonProcessingException e) {
                log.debug("[缓存] 标签列表反序列化失败");
            }
        }

        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getStatus, 1)
                .orderByDesc(Tag::getPostCount);
        List<Tag> tags = tagMapper.selectList(wrapper);

        List<TagVO> vos = tags.stream().map(t -> TagVO.builder()
                .id(t.getId()).name(t.getName()).postCount(t.getPostCount())
                .build()).toList();

        try {
            redisUtils.set(CACHE_KEY, objectMapper.writeValueAsString(vos),
                    CACHE_TTL, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("[缓存] 标签列表序列化失败");
        }
        return vos;
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

        redisUtils.delete(CACHE_KEY);
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

        redisUtils.delete(CACHE_KEY);
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
        long refCount = postTagMapper.selectCount(ptWrapper);
        if (refCount > 0) {
            throw BusinessException.conflict("该标签下关联了 " + refCount + " 篇帖子，请先解除关联后再删除");
        }
        tagMapper.deleteById(id);
        redisUtils.delete(CACHE_KEY);
        log.info("[标签] 删除成功 id={}", id);
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
        redisUtils.delete(CACHE_KEY);
        log.info("[标签] 状态切换 id={} status={}", id, tag.getStatus());
    }
}
