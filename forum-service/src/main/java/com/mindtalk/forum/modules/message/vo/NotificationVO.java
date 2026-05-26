package com.mindtalk.forum.modules.message.vo;

import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;
    private Long userId;
    private UserVO fromUser;
    private String notifyType;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createTime;
}
