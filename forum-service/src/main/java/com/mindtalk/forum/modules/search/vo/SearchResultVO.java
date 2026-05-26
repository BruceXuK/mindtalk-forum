package com.mindtalk.forum.modules.search.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 搜索结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVO {

    private Long id;

    /** 高亮标题 */
    private String title;

    /** 高亮内容摘要 */
    private String content;

    private Long authorId;

    private String authorName;

    private Long categoryId;

    private String categoryName;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Boolean isPinned;

    private Boolean isFeatured;

    private LocalDateTime createTime;
}
