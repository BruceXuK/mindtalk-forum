package com.mindtalk.forum.modules.user.service;

import com.mindtalk.forum.modules.user.dto.*;
import com.mindtalk.forum.modules.user.vo.LoginResultVO;
import com.mindtalk.forum.modules.user.vo.UserProfileVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.util.Date;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 注册 */
    UserVO register(RegisterDTO dto);

    /** 登录 */
    LoginResultVO login(LoginDTO dto);

    /** 刷新令牌 */
    LoginResultVO refreshToken(RefreshTokenDTO dto);

    /** 登出 */
    void logout(Long userId, String tokenJti, Date tokenExp);

    /** 获取当前用户信息 */
    UserVO getCurrentUser(Long userId);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 更新个人资料 */
    UserVO updateProfile(Long userId, UpdateProfileDTO dto);

    /** 上传头像 */
    UserVO uploadAvatar(Long userId, MultipartFile file);

    /** 关注用户 */
    void followUser(Long followerId, Long followeeId);

    /** 取消关注 */
    void unfollowUser(Long followerId, Long followeeId);

    /** 查看用户主页 */
    UserProfileVO getUserProfile(Long userId, Long currentUserId);

    /** 获取关注用户列表（支持模糊搜索） */
    List<UserVO> getFollowingList(Long userId, String keyword, int limit);

    /** 检查用户是否拥有指定角色 */
    boolean hasRole(Long userId, String roleCode);

    /** 搜索用户（按用户名/昵称模糊匹配，用于 @提及） */
    List<UserVO> searchUsers(String keyword, int limit);
}
