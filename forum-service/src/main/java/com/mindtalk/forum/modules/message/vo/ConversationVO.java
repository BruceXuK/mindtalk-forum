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
public class ConversationVO {

    private Long id;

    private Long otherUserId;

    private String otherUsername;

    private String otherNickname;

    private String otherAvatarUrl;

    private String lastMessage;

    private LocalDateTime lastMessageAt;

    private int unreadCount;

    private LocalDateTime createTime;
}
