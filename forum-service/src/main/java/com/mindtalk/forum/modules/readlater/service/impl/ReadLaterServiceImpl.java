package com.mindtalk.forum.modules.readlater.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.readlater.entity.ReadLater;
import com.mindtalk.forum.modules.readlater.mapper.ReadLaterMapper;
import com.mindtalk.forum.modules.readlater.service.ReadLaterService;
import com.mindtalk.forum.modules.readlater.vo.ReadLaterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadLaterServiceImpl implements ReadLaterService {

    private final ReadLaterMapper readLaterMapper;
    private final PostMapper postMapper;

    @Override
    public PageResult<ReadLaterVO> getList(Long userId, int page, int size) {
        IPage<ReadLater> result = readLaterMapper.selectPageWithPost(
                new Page<>(page, size), userId);

        List<ReadLater> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), page, size);
        }

        List<Long> postIds = records.stream()
                .map(ReadLater::getPostId).distinct().collect(Collectors.toList());
        Map<Long, Post> postMap = postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));

        List<ReadLaterVO> vos = records.stream()
                .map(r -> {
                    Post p = postMap.get(r.getPostId());
                    return ReadLaterVO.builder()
                            .id(r.getId())
                            .postId(r.getPostId())
                            .postTitle(p != null ? p.getTitle() : "已删除")
                            .createTime(r.getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void add(Long userId, Long postId) {
        if (readLaterMapper.exists(userId, postId)) {
            return;
        }
        ReadLater rl = ReadLater.builder()
                .userId(userId)
                .postId(postId)
                .build();
        readLaterMapper.insert(rl);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long postId) {
        LambdaQueryWrapper<ReadLater> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadLater::getUserId, userId)
                .eq(ReadLater::getPostId, postId);
        readLaterMapper.delete(wrapper);
    }

    @Override
    public boolean isBookmarked(Long userId, Long postId) {
        return readLaterMapper.exists(userId, postId);
    }
}
