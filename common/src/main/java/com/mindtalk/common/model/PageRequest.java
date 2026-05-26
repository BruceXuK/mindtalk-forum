package com.mindtalk.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分页请求基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码（从 1 开始） */
    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    /** 每页条数 */
    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer size = 10;

    /** 排序字段 */
    private String sort;

    /** 排序方向：asc / desc */
    private String order = "desc";

    /** 计算偏移量（MyBatis Plus 用） */
    public long offset() {
        return (long) (page - 1) * size;
    }
}
