package com.mindtalk.forum.modules.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindtalk.common.constant.Constants;
import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.utils.RedisUtils;
import com.mindtalk.forum.modules.announcement.dto.CreateAnnouncementDTO;
import com.mindtalk.forum.modules.announcement.entity.Announcement;
import com.mindtalk.forum.modules.announcement.mapper.AnnouncementMapper;
import com.mindtalk.forum.modules.announcement.service.AnnouncementService;
import com.mindtalk.forum.modules.announcement.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final RedisUtils redisUtils;

    private static final String CACHE_ACTIVE_KEY = Constants.REDIS_PREFIX + "announcement:active";
    private static final long CACHE_TTL = 10;

    @Override
    public List<AnnouncementVO> listAll() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectList(wrapper).stream()
                .map(this::toVO).toList();
    }

    @Override
    public List<AnnouncementVO> listActive() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 1)
                .and(w -> w.isNull(Announcement::getExpireTime)
                        .or().gt(Announcement::getExpireTime, LocalDateTime.now()))
                .orderByDesc(Announcement::getIsPinned)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectList(wrapper).stream()
                .map(this::toVO).toList();
    }

    @Override
    @Transactional
    public AnnouncementVO create(CreateAnnouncementDTO dto, Long userId) {
        Announcement announcement = Announcement.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .summary(dto.getSummary())
                .level(dto.getLevel() != null ? dto.getLevel() : "INFO")
                .isPinned(dto.getIsPinned() != null && dto.getIsPinned())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .viewCount(0)
                .status(0)
                .createdBy(userId)
                .build();
        if (dto.getExpireTime() != null && !dto.getExpireTime().isBlank()) {
            announcement.setExpireTime(LocalDateTime.parse(dto.getExpireTime()));
        }
        announcementMapper.insert(announcement);
        redisUtils.delete(CACHE_ACTIVE_KEY);
        log.info("[公告] 创建成功 id={} title={}", announcement.getId(), announcement.getTitle());
        return toVO(announcement);
    }

    @Override
    @Transactional
    public AnnouncementVO update(Long id, CreateAnnouncementDTO dto) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        if (dto.getTitle() != null) announcement.setTitle(dto.getTitle());
        if (dto.getContent() != null) announcement.setContent(dto.getContent());
        if (dto.getSummary() != null) announcement.setSummary(dto.getSummary());
        if (dto.getLevel() != null) announcement.setLevel(dto.getLevel());
        if (dto.getIsPinned() != null) announcement.setIsPinned(dto.getIsPinned());
        if (dto.getSortOrder() != null) announcement.setSortOrder(dto.getSortOrder());
        if (dto.getExpireTime() != null) {
            announcement.setExpireTime(dto.getExpireTime().isBlank() ? null
                    : LocalDateTime.parse(dto.getExpireTime()));
        }
        announcementMapper.updateById(announcement);
        redisUtils.delete(CACHE_ACTIVE_KEY);
        log.info("[公告] 编辑成功 id={}", id);
        return toVO(announcement);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        announcementMapper.deleteById(id);
        redisUtils.delete(CACHE_ACTIVE_KEY);
        log.info("[公告] 删除成功 id={}", id);
    }

    @Override
    @Transactional
    public void togglePublish(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        if (announcement.getStatus() == 1) {
            announcement.setStatus(2);
        } else {
            announcement.setStatus(1);
            announcement.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.updateById(announcement);
        redisUtils.delete(CACHE_ACTIVE_KEY);
        log.info("[公告] 状态切换 id={} status={}", id, announcement.getStatus());
    }

    @Override
    @Transactional
    public void togglePin(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw BusinessException.notFound("公告不存在");
        }
        announcement.setIsPinned(!announcement.getIsPinned());
        announcementMapper.updateById(announcement);
        redisUtils.delete(CACHE_ACTIVE_KEY);
        log.info("[公告] 置顶切换 id={} isPinned={}", id, announcement.getIsPinned());
    }

    private AnnouncementVO toVO(Announcement a) {
        return AnnouncementVO.builder()
                .id(a.getId()).title(a.getTitle()).content(a.getContent())
                .summary(a.getSummary()).level(a.getLevel()).status(a.getStatus())
                .isPinned(a.getIsPinned()).publishTime(a.getPublishTime())
                .expireTime(a.getExpireTime()).sortOrder(a.getSortOrder())
                .viewCount(a.getViewCount()).createdBy(a.getCreatedBy())
                .createTime(a.getCreateTime()).updateTime(a.getUpdateTime())
                .build();
    }
}
