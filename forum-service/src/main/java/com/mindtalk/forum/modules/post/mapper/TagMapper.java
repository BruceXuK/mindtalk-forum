package com.mindtalk.forum.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.post.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 查询帖子的标签 */
    List<Tag> selectByPostId(@Param("postId") Long postId);
}
