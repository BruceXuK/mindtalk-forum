package com.mindtalk.forum.modules.series.dto;

import lombok.Data;

@Data
public class UpdateSeriesDTO {
    private String title;
    private String description;
    private String coverUrl;
    private Integer status;
}
