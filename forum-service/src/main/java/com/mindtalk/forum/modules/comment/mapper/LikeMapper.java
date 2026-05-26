package com.mindtalk.forum.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.comment.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    /** 是否已点赞 */
    int exists(@Param("userId") Long userId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId);

    /** 查询目标点赞数 */
    int countByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    /** 批量查询当前用户已点赞的目标 ID 集合 */
    List<Long> selectLikedIds(@Param("userId") Long userId,
                              @Param("targetType") String targetType,
                              @Param("targetIds") List<Long> targetIds);
}
