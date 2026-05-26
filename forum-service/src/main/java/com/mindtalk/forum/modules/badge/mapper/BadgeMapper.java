package com.mindtalk.forum.modules.badge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.badge.entity.Badge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BadgeMapper extends BaseMapper<Badge> {

    List<Badge> selectByUserId(@Param("userId") Long userId);
}
