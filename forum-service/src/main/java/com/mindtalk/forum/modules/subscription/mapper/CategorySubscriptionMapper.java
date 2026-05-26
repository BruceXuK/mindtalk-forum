package com.mindtalk.forum.modules.subscription.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.subscription.entity.CategorySubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategorySubscriptionMapper extends BaseMapper<CategorySubscription> {

    boolean exists(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    List<Long> selectSubscribedCategoryIds(@Param("userId") Long userId);
}
