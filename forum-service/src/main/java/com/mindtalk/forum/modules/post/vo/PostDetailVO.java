package com.mindtalk.forum.modules.post.vo;

import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailVO {

    private Long id;

    private String title;

    private String content;

    private String contentText;

    private UserVO author;

    private CategoryVO category;

    private List<TagVO> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private Boolean isPinned;

    private Boolean isFeatured;

    private LocalDateTime pinnedUntil;

    private LocalDateTime featuredUntil;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否已收藏 */
    private Boolean isCollected;

    /** 当前用户是否已关注作者 */
    private Boolean authorIsFollowing;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
