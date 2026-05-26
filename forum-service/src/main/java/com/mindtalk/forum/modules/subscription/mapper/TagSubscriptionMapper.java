package com.mindtalk.forum.modules.subscription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.subscription.entity.TagSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagSubscriptionMapper extends BaseMapper<TagSubscription> {

    boolean exists(@Param("userId") Long userId, @Param("tagId") Long tagId);

    List<Long> selectSubscribedTagIds(@Param("userId") Long userId);
}
