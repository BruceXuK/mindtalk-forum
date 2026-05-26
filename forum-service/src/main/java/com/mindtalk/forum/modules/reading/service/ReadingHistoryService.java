package com.mindtalk.forum.modules.reading.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.reading.vo.ReadingHistoryVO;

public interface ReadingHistoryService {

    PageResult<ReadingHistoryVO> getList(Long userId, int page, int size);

    void record(Long userId, Long postId);

    void delete(Long userId, Long id);

    void clearAll(Long userId);
}
