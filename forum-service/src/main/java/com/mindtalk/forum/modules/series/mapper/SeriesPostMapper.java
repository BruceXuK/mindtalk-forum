package com.mindtalk.forum.modules.series.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.series.entity.SeriesPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeriesPostMapper extends BaseMapper<SeriesPost> {

    /** 根据系列 ID 按排序号查询帖子 ID 列表 */
    List<Long> selectPostIdsBySeriesId(@Param("seriesId") Long seriesId);

    /** 根据帖子 ID 查询所属系列 ID */
    List<Long> selectSeriesIdsByPostId(@Param("postId") Long postId);
}
