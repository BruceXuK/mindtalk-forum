package com.mindtalk.forum.modules.badge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.badge.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {

    boolean exists(@Param("userId") Long userId, @Param("badgeId") Long badgeId);
}
