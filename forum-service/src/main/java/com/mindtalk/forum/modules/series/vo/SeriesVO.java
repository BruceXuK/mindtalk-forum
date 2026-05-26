package com.mindtalk.forum.modules.series.vo;

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
public class SeriesVO {

    private Long id;

    private UserVO author;

    private String title;

    private String description;

    private String coverUrl;

    private Integer postCount;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
