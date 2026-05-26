package com.mindtalk.forum.modules.post.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreateCategoryDTO;
import com.mindtalk.forum.modules.post.service.CategoryService;
import com.mindtalk.forum.modules.post.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类接口
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.list());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> create(@Valid @RequestBody CreateCategoryDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CategoryVO> update(@PathVariable Long id,
                                      @Valid @RequestBody CreateCategoryDTO dto) {
        return Result.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
