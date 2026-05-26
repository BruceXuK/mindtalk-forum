package com.mindtalk.forum.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindtalk.forum.modules.message.entity.NotificationSetting;
import com.mindtalk.forum.modules.message.mapper.NotificationSettingMapper;
import com.mindtalk.forum.modules.message.service.NotificationSettingService;
import com.mindtalk.forum.modules.message.vo.NotificationSettingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingMapper settingMapper;

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "LIKE", "点赞通知",
            "COMMENT", "评论通知",
            "FOLLOW", "关注通知",
            "MENTION", "@提及通知",
            "SYSTEM", "系统通知"
    );

    private static final List<String> ALL_TYPES = List.of("LIKE", "COMMENT", "FOLLOW", "MENTION", "SYSTEM");

    @Override
    public List<NotificationSettingVO> getSettings(Long userId) {
        List<NotificationSetting> settings = settingMapper.selectByUserId(userId);
        Map<String, Boolean> enabledMap = settings.stream()
                .collect(Collectors.toMap(NotificationSetting::getNotifyType, s -> s.getEnabled() != null && s.getEnabled()));

        return ALL_TYPES.stream()
                .map(type -> NotificationSettingVO.builder()
                        .notifyType(type)
                        .label(TYPE_LABELS.getOrDefault(type, type))
                        .enabled(enabledMap.getOrDefault(type, true))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateSetting(Long userId, String notifyType, boolean enabled) {
        LambdaQueryWrapper<NotificationSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationSetting::getUserId, userId)
                .eq(NotificationSetting::getNotifyType, notifyType);
        NotificationSetting existing = settingMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setEnabled(enabled);
            settingMapper.updateById(existing);
        } else {
            settingMapper.insert(NotificationSetting.builder()
                    .userId(userId)
                    .notifyType(notifyType)
                    .enabled(enabled)
                    .build());
        }
    }

    @Override
    public boolean isEnabled(Long userId, String notifyType) {
        return settingMapper.isEnabled(userId, notifyType);
    }
}
