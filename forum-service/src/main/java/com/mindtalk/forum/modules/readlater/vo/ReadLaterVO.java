package com.mindtalk.forum.modules.readlater.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadLaterVO {

    private Long id;

    private Long postId;

    private String postTitle;

    private LocalDateTime createTime;
}
