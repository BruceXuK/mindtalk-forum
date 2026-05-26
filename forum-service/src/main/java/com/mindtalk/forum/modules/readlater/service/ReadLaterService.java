package com.mindtalk.forum.modules.readlater.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.readlater.vo.ReadLaterVO;

public interface ReadLaterService {

    PageResult<ReadLaterVO> getList(Long userId, int page, int size);

    void add(Long userId, Long postId);

    void remove(Long userId, Long postId);

    boolean isBookmarked(Long userId, Long postId);
}
