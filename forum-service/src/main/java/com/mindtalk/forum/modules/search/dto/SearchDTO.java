package com.mindtalk.forum.modules.search.dto;

import com.mindtalk.common.model.PageRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 搜索请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchDTO extends PageRequest {

    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 分类 ID 筛选 */
    private Long categoryId;

    /** 作者 ID 筛选 */
    private Long userId;

    /** 时间范围起始 */
    private String dateFrom;

    /** 时间范围截止 */
    private String dateTo;
}
