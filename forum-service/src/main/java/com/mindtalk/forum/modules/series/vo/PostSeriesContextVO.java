package com.mindtalk.forum.modules.series.vo;

import com.mindtalk.forum.modules.post.vo.PostVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSeriesContextVO {

    private SeriesVO series;

    private PostNavigationVO prevPost;

    private PostNavigationVO nextPost;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostNavigationVO {
        private Long id;
        private String title;
    }
}
