package com.mindtalk.forum.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.post.entity.PostTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostTagMapper extends BaseMapper<PostTag> {

    /** 批量插入帖子标签关联 */
    int batchInsert(@Param("list") List<PostTag> list);

    /** 按帖子 ID 删除所有标签关联 */
    int deleteByPostId(@Param("postId") Long postId);

    /** 查询帖子的标签关联 */
    List<PostTag> selectByPostId(@Param("postId") Long postId);

    /** 批量查询帖子的标签关联 */
    List<PostTag> selectByPostIds(@Param("postIds") List<Long> postIds);
}
