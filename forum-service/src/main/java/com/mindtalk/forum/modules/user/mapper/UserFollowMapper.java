package com.mindtalk.forum.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注 Mapper（自定义 SQL 见 resources/mapper/UserFollowMapper.xml）
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /** 查询关注数 */
    int countFollowing(@Param("userId") Long userId);

    /** 查询粉丝数 */
    int countFollower(@Param("userId") Long userId);

    /** 是否已关注 */
    int isFollowing(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 批量查询当前用户已关注的用户ID列表 */
    List<Long> selectFolloweeIdsByFollower(@Param("followerId") Long followerId, @Param("followeeIds") List<Long> followeeIds);

    /** 分页查询关注用户列表（支持昵称/用户名模糊搜索） */
    List<User> selectFollowingUsers(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") int limit);
}
