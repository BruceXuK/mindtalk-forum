package com.mindtalk.forum.modules.reading.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistoryVO {

    private Long id;

    private Long postId;

    private String postTitle;

    private LocalDateTime readAt;

    private LocalDateTime createTime;
}
