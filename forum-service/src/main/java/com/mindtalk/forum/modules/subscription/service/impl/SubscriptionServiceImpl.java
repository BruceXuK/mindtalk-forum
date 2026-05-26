package com.mindtalk.forum.modules.subscription.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindtalk.forum.modules.subscription.entity.CategorySubscription;
import com.mindtalk.forum.modules.subscription.entity.TagSubscription;
import com.mindtalk.forum.modules.subscription.mapper.CategorySubscriptionMapper;
import com.mindtalk.forum.modules.subscription.mapper.TagSubscriptionMapper;
import com.mindtalk.forum.modules.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TagSubscriptionMapper tagSubscriptionMapper;
    private final CategorySubscriptionMapper categorySubscriptionMapper;

    @Override
    @Transactional
    public void subscribeTag(Long userId, Long tagId) {
        if (tagSubscriptionMapper.exists(userId, tagId)) return;
        TagSubscription sub = TagSubscription.builder()
                .userId(userId).tagId(tagId).build();
        tagSubscriptionMapper.insert(sub);
    }

    @Override
    @Transactional
    public void unsubscribeTag(Long userId, Long tagId) {
        LambdaQueryWrapper<TagSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagSubscription::getUserId, userId)
                .eq(TagSubscription::getTagId, tagId);
        tagSubscriptionMapper.delete(wrapper);
    }

    @Override
    public boolean isTagSubscribed(Long userId, Long tagId) {
        return tagSubscriptionMapper.exists(userId, tagId);
    }

    @Override
    public List<Long> getSubscribedTagIds(Long userId) {
        List<Long> ids = tagSubscriptionMapper.selectSubscribedTagIds(userId);
        return ids != null ? ids : Collections.emptyList();
    }

    @Override
    @Transactional
    public void subscribeCategory(Long userId, Long categoryId) {
        if (categorySubscriptionMapper.exists(userId, categoryId)) return;
        CategorySubscription sub = CategorySubscription.builder()
                .userId(userId).categoryId(categoryId).build();
        categorySubscriptionMapper.insert(sub);
    }

    @Override
    @Transactional
    public void unsubscribeCategory(Long userId, Long categoryId) {
        LambdaQueryWrapper<CategorySubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategorySubscription::getUserId, userId)
                .eq(CategorySubscription::getCategoryId, categoryId);
        categorySubscriptionMapper.delete(wrapper);
    }

    @Override
    public boolean isCategorySubscribed(Long userId, Long categoryId) {
        return categorySubscriptionMapper.exists(userId, categoryId);
    }

    @Override
    public List<Long> getSubscribedCategoryIds(Long userId) {
        List<Long> ids = categorySubscriptionMapper.selectSubscribedCategoryIds(userId);
        return ids != null ? ids : Collections.emptyList();
    }
}
