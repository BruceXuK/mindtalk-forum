package com.mindtalk.forum.modules.subscription.service;

import java.util.List;

public interface SubscriptionService {

    void subscribeTag(Long userId, Long tagId);

    void unsubscribeTag(Long userId, Long tagId);

    boolean isTagSubscribed(Long userId, Long tagId);

    List<Long> getSubscribedTagIds(Long userId);

    void subscribeCategory(Long userId, Long categoryId);

    void unsubscribeCategory(Long userId, Long categoryId);

    boolean isCategorySubscribed(Long userId, Long categoryId);

    List<Long> getSubscribedCategoryIds(Long userId);
}
