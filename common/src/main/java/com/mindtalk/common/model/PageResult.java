package com.mindtalk.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> records;

    /** 总条数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;

    /** 总页数 */
    private Integer totalPages;

    // ──────────────────── 构建 ────────────────────

    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, totalPages);
    }

    public static <T> PageResult<T> empty(Integer page, Integer size) {
        return new PageResult<>(Collections.emptyList(), 0L, page, size, 0);
    }

    /**
     * 类型转换（常用于 Entity → VO）
     */
    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> converted = this.records.stream()
                .map(converter)
                .collect(Collectors.toList());
        return new PageResult<>(converted, this.total, this.page, this.size, this.totalPages);
    }
}
