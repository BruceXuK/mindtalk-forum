package com.mindtalk.forum.modules.message.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String content;

    private Boolean isRead;

    private LocalDateTime readAt;

    private LocalDateTime createTime;
}
