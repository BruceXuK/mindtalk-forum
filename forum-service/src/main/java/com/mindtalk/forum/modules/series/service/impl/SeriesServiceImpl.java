package com.mindtalk.forum.modules.series.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.post.vo.CategoryVO;
import com.mindtalk.forum.modules.post.vo.PostVO;
import com.mindtalk.forum.modules.post.vo.TagVO;
import com.mindtalk.forum.modules.series.dto.CreateSeriesDTO;
import com.mindtalk.forum.modules.series.dto.UpdateSeriesDTO;
import com.mindtalk.forum.modules.series.entity.Series;
import com.mindtalk.forum.modules.series.entity.SeriesPost;
import com.mindtalk.forum.modules.series.mapper.SeriesMapper;
import com.mindtalk.forum.modules.series.mapper.SeriesPostMapper;
import com.mindtalk.forum.modules.series.service.SeriesService;
import com.mindtalk.forum.modules.series.vo.PostSeriesContextVO;
import com.mindtalk.forum.modules.series.vo.SeriesDetailVO;
import com.mindtalk.forum.modules.series.vo.SeriesVO;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {

    private final SeriesMapper seriesMapper;
    private final SeriesPostMapper seriesPostMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public SeriesDetailVO createSeries(Long userId, CreateSeriesDTO dto) {
        Series series = Series.builder()
                .authorId(userId)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .coverUrl(dto.getCoverUrl())
                .postCount(0)
                .sortOrder(0)
                .status(1)
                .build();
        seriesMapper.insert(series);

        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
            addPostsInternal(series.getId(), dto.getPostIds());
            series.setPostCount(dto.getPostIds().size());
            seriesMapper.updateById(series);
        }

        log.info("[系列] 创建成功 id={} userId={} title={}", series.getId(), userId, series.getTitle());
        return buildDetailVO(series, userId);
    }

    @Override
    @Transactional
    public SeriesDetailVO updateSeries(Long userId, Long seriesId, UpdateSeriesDTO dto) {
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw BusinessException.notFound("系列不存在");
        }
        if (!series.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("无权编辑该系列");
        }

        if (dto.getTitle() != null) series.setTitle(dto.getTitle());
        if (dto.getDescription() != null) series.setDescription(dto.getDescription());
        if (dto.getCoverUrl() != null) series.setCoverUrl(dto.getCoverUrl());
        if (dto.getStatus() != null) series.setStatus(dto.getStatus());
        seriesMapper.updateById(series);

        log.info("[系列] 编辑成功 id={}", seriesId);
        return buildDetailVO(series, userId);
    }

    @Override
    @Transactional
    public void deleteSeries(Long userId, Long seriesId) {
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw BusinessException.notFound("系列不存在");
        }
        if (!series.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("无权删除该系列");
        }

        LambdaQueryWrapper<SeriesPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeriesPost::getSeriesId, seriesId);
        seriesPostMapper.delete(wrapper);
        seriesMapper.deleteById(seriesId);

        log.info("[系列] 删除成功 id={}", seriesId);
    }

    @Override
    public SeriesDetailVO getSeriesDetail(Long seriesId) {
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw BusinessException.notFound("系列不存在");
        }
        return buildDetailVO(series, null);
    }

    @Override
    public PageResult<SeriesVO> getUserSeries(Long userId, int page, int size) {
        LambdaQueryWrapper<Series> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Series::getAuthorId, userId)
                .eq(Series::getStatus, 1)
                .orderByDesc(Series::getCreateTime);

        IPage<Series> result = seriesMapper.selectPage(new Page<>(page, size), wrapper);
        List<SeriesVO> vos = result.getRecords().stream()
                .map(s -> buildSeriesVO(s))
                .toList();
        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public List<SeriesVO> getMySeries(Long userId) {
        LambdaQueryWrapper<Series> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Series::getAuthorId, userId)
                .orderByDesc(Series::getCreateTime);

        List<Series> list = seriesMapper.selectList(wrapper);
        return list.stream().map(this::buildSeriesVO).toList();
    }

    @Override
    @Transactional
    public void addPost(Long userId, Long seriesId, Long postId) {
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw BusinessException.notFound("系列不存在");
        }
        if (!series.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("无权编辑该系列");
        }

        LambdaQueryWrapper<SeriesPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeriesPost::getSeriesId, seriesId)
                .eq(SeriesPost::getPostId, postId);
        if (seriesPostMapper.selectCount(wrapper) > 0) {
            throw BusinessException.conflict("帖子已在系列中");
        }

        SeriesPost sp = SeriesPost.builder()
                .seriesId(seriesId)
                .postId(postId)
                .sortOrder(series.getPostCount())
                .build();
        seriesPostMapper.insert(sp);

        series.setPostCount(series.getPostCount() + 1);
        seriesMapper.updateById(series);
        log.info("[系列] 添加帖子 seriesId={} postId={}", seriesId, postId);
    }

    @Override
    @Transactional
    public void removePost(Long userId, Long seriesId, Long postId) {
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw BusinessException.notFound("系列不存在");
        }
        if (!series.getAuthorId().equals(userId)) {
            throw BusinessException.forbidden("无权编辑该系列");
        }

        LambdaQueryWrapper<SeriesPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeriesPost::getSeriesId, seriesId)
                .eq(SeriesPost::getPostId, postId);
        seriesPostMapper.delete(wrapper);

        series.setPostCount(Math.max(0, series.getPostCount() - 1));
        seriesMapper.updateById(series);
        log.info("[系列] 移除帖子 seriesId={} postId={}", seriesId, postId);
    }

    @Override
    public PostSeriesContextVO getPostSeriesContext(Long postId) {
        List<Long> seriesIds = seriesPostMapper.selectSeriesIdsByPostId(postId);
        if (seriesIds == null || seriesIds.isEmpty()) {
            return null;
        }

        Long seriesId = seriesIds.get(0);
        Series series = seriesMapper.selectById(seriesId);
        if (series == null) {
            return null;
        }

        List<Long> postIds = seriesPostMapper.selectPostIdsBySeriesId(seriesId);
        int idx = postIds.indexOf(postId);

        PostSeriesContextVO.PostNavigationVO prevPost = null;
        PostSeriesContextVO.PostNavigationVO nextPost = null;

        if (idx > 0) {
            Long prevId = postIds.get(idx - 1);
            Post prev = postMapper.selectById(prevId);
            if (prev != null) {
                prevPost = PostSeriesContextVO.PostNavigationVO.builder()
                        .id(prev.getId()).title(prev.getTitle()).build();
            }
        }
        if (idx >= 0 && idx < postIds.size() - 1) {
            Long nextId = postIds.get(idx + 1);
            Post next = postMapper.selectById(nextId);
            if (next != null) {
                nextPost = PostSeriesContextVO.PostNavigationVO.builder()
                        .id(next.getId()).title(next.getTitle()).build();
            }
        }

        return PostSeriesContextVO.builder()
                .series(buildSeriesVO(series))
                .prevPost(prevPost)
                .nextPost(nextPost)
                .build();
    }

    // ──────────────────── 内部方法 ────────────────────

    private void addPostsInternal(Long seriesId, List<Long> postIds) {
        int order = 0;
        for (Long postId : postIds) {
            SeriesPost sp = SeriesPost.builder()
                    .seriesId(seriesId)
                    .postId(postId)
                    .sortOrder(order++)
                    .build();
            seriesPostMapper.insert(sp);
        }
    }

    private SeriesVO buildSeriesVO(Series s) {
        UserVO author = buildSimpleUserVO(s.getAuthorId());
        return SeriesVO.builder()
                .id(s.getId())
                .author(author)
                .title(s.getTitle())
                .description(s.getDescription())
                .coverUrl(s.getCoverUrl())
                .postCount(s.getPostCount())
                .sortOrder(s.getSortOrder())
                .createTime(s.getCreateTime())
                .updateTime(s.getUpdateTime())
                .build();
    }

    private SeriesDetailVO buildDetailVO(Series series, Long currentUserId) {
        UserVO author = buildSimpleUserVO(series.getAuthorId());

        List<Long> postIds = seriesPostMapper.selectPostIdsBySeriesId(series.getId());
        List<PostVO> posts = Collections.emptyList();
        if (postIds != null && !postIds.isEmpty()) {
            List<Post> postList = postMapper.selectBatchIds(postIds);
            Map<Long, Post> postMap = postList.stream()
                    .collect(Collectors.toMap(Post::getId, p -> p));
            posts = postIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .map(p -> buildSimplePostVO(p))
                    .toList();
        }

        return SeriesDetailVO.builder()
                .id(series.getId())
                .author(author)
                .title(series.getTitle())
                .description(series.getDescription())
                .coverUrl(series.getCoverUrl())
                .postCount(series.getPostCount())
                .posts(posts)
                .createTime(series.getCreateTime())
                .updateTime(series.getUpdateTime())
                .build();
    }

    private UserVO buildSimpleUserVO(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return UserVO.builder().id(userId).nickname("未知用户").build();
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .gender(user.getGender())
                .location(user.getLocation())
                .createTime(user.getCreateTime())
                .build();
    }

    private PostVO buildSimplePostVO(Post p) {
        String summary = p.getContentText() != null && p.getContentText().length() > 200
                ? p.getContentText().substring(0, 200)
                : p.getContentText();
        return PostVO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .summary(summary)
                .viewCount(p.getViewCount())
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .collectCount(p.getCollectCount())
                .isPinned(p.getIsPinned())
                .isFeatured(p.getIsFeatured())
                .pinnedUntil(p.getPinnedUntil())
                .featuredUntil(p.getFeaturedUntil())
                .status(p.getStatus())
                .createTime(p.getCreateTime())
                .updateTime(p.getUpdateTime())
                .build();
    }
}
