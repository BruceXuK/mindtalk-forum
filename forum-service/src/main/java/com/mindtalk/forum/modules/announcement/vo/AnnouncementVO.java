package com.mindtalk.forum.modules.announcement.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementVO {

    private Long id;

    private String title;

    private String content;

    private String summary;

    private String level;

    private Integer status;

    private Boolean isPinned;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private Integer sortOrder;

    private Integer viewCount;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
