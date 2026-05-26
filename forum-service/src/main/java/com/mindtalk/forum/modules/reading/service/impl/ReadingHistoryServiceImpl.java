package com.mindtalk.forum.modules.reading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.reading.entity.ReadingHistory;
import com.mindtalk.forum.modules.reading.mapper.ReadingHistoryMapper;
import com.mindtalk.forum.modules.reading.service.ReadingHistoryService;
import com.mindtalk.forum.modules.reading.vo.ReadingHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingHistoryServiceImpl implements ReadingHistoryService {

    private final ReadingHistoryMapper readingHistoryMapper;
    private final PostMapper postMapper;

    @Override
    public PageResult<ReadingHistoryVO> getList(Long userId, int page, int size) {
        IPage<ReadingHistory> result = readingHistoryMapper.selectPageWithPost(
                new Page<>(page, size), userId);

        List<ReadingHistory> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), page, size);
        }

        List<Long> postIds = records.stream()
                .map(ReadingHistory::getPostId).distinct().collect(Collectors.toList());
        Map<Long, Post> postMap = postMapper.selectBatchIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));

        List<ReadingHistoryVO> vos = records.stream()
                .map(r -> {
                    Post p = postMap.get(r.getPostId());
                    return ReadingHistoryVO.builder()
                            .id(r.getId())
                            .postId(r.getPostId())
                            .postTitle(p != null ? p.getTitle() : "已删除")
                            .readAt(r.getReadAt())
                            .createTime(r.getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void record(Long userId, Long postId) {
        LambdaQueryWrapper<ReadingHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingHistory::getUserId, userId)
                .eq(ReadingHistory::getPostId, postId);
        ReadingHistory existing = readingHistoryMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setReadAt(LocalDateTime.now());
            readingHistoryMapper.updateById(existing);
        } else {
            ReadingHistory rh = ReadingHistory.builder()
                    .userId(userId)
                    .postId(postId)
                    .readAt(LocalDateTime.now())
                    .build();
            readingHistoryMapper.insert(rh);
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        ReadingHistory rh = readingHistoryMapper.selectById(id);
        if (rh != null && rh.getUserId().equals(userId)) {
            readingHistoryMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void clearAll(Long userId) {
        LambdaUpdateWrapper<ReadingHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReadingHistory::getUserId, userId)
                .set(ReadingHistory::getDeleted, 1);
        readingHistoryMapper.update(null, wrapper);
    }
}
