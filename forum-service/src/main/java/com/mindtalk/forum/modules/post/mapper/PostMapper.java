package com.mindtalk.forum.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.forum.modules.post.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /** 分页查询帖子列表（连作者/分类） */
    IPage<Post> selectPageWithAuthor(Page<Post> page,
                                     @Param("categoryId") Long categoryId,
                                     @Param("tagId") Long tagId,
                                     @Param("keyword") String keyword,
                                     @Param("status") Integer status,
                                     @Param("orderBy") String orderBy,
                                     @Param("userId") Long userId,
                                     @Param("followingUserId") Long followingUserId);

    /** 热门帖子 */
    List<Post> selectHotPosts(@Param("limit") int limit);

    List<Post> selectRanking(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);
}
