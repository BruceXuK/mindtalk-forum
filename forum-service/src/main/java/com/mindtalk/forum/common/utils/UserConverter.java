package com.mindtalk.forum.common.utils;

import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.vo.UserVO;

/**
 * 用户 Entity → VO 统一转换器
 */
public final class UserConverter {

    private UserConverter() {}

    /**
     * User 实体转 UserVO（基础字段，不含统计计数）
     */
    public static UserVO toUserVO(User user) {
        if (user == null) return null;
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
}
