package com.mindtalk.forum.modules.reading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.forum.modules.reading.entity.ReadingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadingHistoryMapper extends BaseMapper<ReadingHistory> {

    IPage<ReadingHistory> selectPageWithPost(Page<ReadingHistory> page, @Param("userId") Long userId);
}
