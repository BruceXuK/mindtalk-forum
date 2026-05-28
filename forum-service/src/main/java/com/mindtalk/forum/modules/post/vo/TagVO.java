package com.mindtalk.forum.modules.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    private Long id;

    private String name;

    private String description;

    private Integer postCount;

    private Integer status;
}
