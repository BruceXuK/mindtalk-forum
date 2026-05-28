package com.mindtalk.forum.modules.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAnnouncementDTO {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "标题最长 200 个字符")
    private String title;

    private String content;

    @Size(max = 500, message = "摘要最长 500 个字符")
    private String summary;

    private String level;

    private Boolean isPinned;

    private String expireTime;

    private Integer sortOrder;
}
