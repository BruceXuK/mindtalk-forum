package com.mindtalk.forum.modules.badge.service;

import com.mindtalk.forum.modules.badge.vo.BadgeVO;

import java.util.List;

public interface BadgeService {

    List<BadgeVO> getUserBadges(Long userId);

    void evaluateAndUnlock(Long userId);

    void evaluateAllUsers();
}
