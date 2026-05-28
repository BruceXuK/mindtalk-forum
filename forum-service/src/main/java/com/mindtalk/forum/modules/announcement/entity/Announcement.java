package com.mindtalk.forum.modules.announcement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("announcements")
public class Announcement {

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
