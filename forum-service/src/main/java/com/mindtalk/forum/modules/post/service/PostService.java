package com.mindtalk.forum.modules.post.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.post.dto.CreatePostDTO;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.dto.UpdatePostDTO;
import com.mindtalk.forum.modules.post.vo.PostDetailVO;
import com.mindtalk.forum.modules.post.vo.PostVO;

import java.util.List;

/**
 * 帖子服务接口
 */
public interface PostService {

    /** 发帖 */
    PostDetailVO createPost(Long authorId, CreatePostDTO dto);

    /** 编辑帖子 */
    PostDetailVO updatePost(Long userId, Long postId, UpdatePostDTO dto);

    /** 删除帖子（软删除） */
    void deletePost(Long userId, Long postId);

    /** 分页查询 */
    PageResult<PostVO> getPostPage(PostQueryDTO query, Long currentUserId);

    /** 帖子详情 */
    PostDetailVO getPostDetail(Long postId, Long currentUserId);

    /** 热门帖子 */
    List<PostVO> getHotPosts(int limit);

    /** 记录浏览（异步） */
    void recordView(Long postId);

    /** 点赞/取消点赞 */
    void likePost(Long userId, Long postId);

    /** 收藏/取消收藏 */
    void collectPost(Long userId, Long postId);

    /** 获取我的草稿列表 */
    PageResult<PostVO> getMyDrafts(Long userId, int page, int size);

    /** 发布草稿 */
    PostDetailVO publishDraft(Long userId, Long postId);

    /** 个性化推荐 */
    List<PostVO> getRecommendedPosts(Long userId, int limit);

    /** 排行榜（本周/本月） */
    List<PostVO> getRankingPosts(String period, int limit);

    /** 相似帖子推荐（基于标签重叠度） */
    List<PostVO> getSimilarPosts(Long postId, int limit);
}
