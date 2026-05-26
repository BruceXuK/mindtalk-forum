package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsTrendVO {

    private String date;

    private Long newUsers;

    private Long newPosts;

    private Long newComments;
}
