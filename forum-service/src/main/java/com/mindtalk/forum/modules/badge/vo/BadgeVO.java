package com.mindtalk.forum.modules.badge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeVO {

    private Long id;

    private String code;

    private String name;

    private String description;

    private String iconUrl;

    private String category;

    private Integer sortOrder;

    private LocalDateTime unlockedAt;
}
