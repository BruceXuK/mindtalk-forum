package com.mindtalk.forum.modules.message.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingVO {

    private String notifyType;

    private String label;

    private Boolean enabled;
}
