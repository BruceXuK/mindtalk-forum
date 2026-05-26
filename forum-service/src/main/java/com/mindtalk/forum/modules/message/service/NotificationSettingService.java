package com.mindtalk.forum.modules.message.service;

import com.mindtalk.forum.modules.message.vo.NotificationSettingVO;

import java.util.List;

public interface NotificationSettingService {

    List<NotificationSettingVO> getSettings(Long userId);

    void updateSetting(Long userId, String notifyType, boolean enabled);

    boolean isEnabled(Long userId, String notifyType);
}
