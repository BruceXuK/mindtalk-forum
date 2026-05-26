package com.mindtalk.forum.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.forum.modules.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 分页查询一级评论（parent_id IS NULL） */
    IPage<Comment> selectFirstLevelPage(Page<Comment> page, @Param("postId") Long postId, @Param("orderBy") String orderBy);

    /** 查询子回复 */
    List<Comment> selectReplies(@Param("parentId") Long parentId, @Param("limit") int limit);

    /** 批量查询子回复（按多个父评论 ID） */
    List<Comment> selectRepliesByParentIds(@Param("parentIds") List<Long> parentIds, @Param("limit") int limit);

    /** 统计子回复数 */
    int countReplies(@Param("parentId") Long parentId);

    /** 批量统计子回复数 */
    List<Map<String, Object>> countRepliesByParentIds(@Param("parentIds") List<Long> parentIds);
}
