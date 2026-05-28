package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreateCategoryDTO;
import com.mindtalk.forum.modules.post.service.CategoryService;
import com.mindtalk.forum.modules.post.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端 - 分类管理
 */
@Tag(name = "管理端-分类管理")
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表（含禁用）")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<CategoryVO> create(@Valid @RequestBody CreateCategoryDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<CategoryVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CreateCategoryDTO dto) {
        return Result.ok(categoryService.update(id, dto));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换分类启用/禁用")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        categoryService.toggleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "批量更新排序")
    @PutMapping("/batch-sort")
    public Result<Void> batchSort(@RequestBody List<Map<String, Object>> items) {
        categoryService.batchSort(items);
        return Result.ok();
    }
}
