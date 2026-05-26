package com.mindtalk.forum.modules.readlater.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.forum.modules.readlater.entity.ReadLater;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReadLaterMapper extends BaseMapper<ReadLater> {

    IPage<ReadLater> selectPageWithPost(Page<ReadLater> page, @Param("userId") Long userId);

    boolean exists(@Param("userId") Long userId, @Param("postId") Long postId);
}
