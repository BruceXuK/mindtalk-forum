package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreateTagDTO;
import com.mindtalk.forum.modules.post.service.TagService;
import com.mindtalk.forum.modules.post.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端 - 标签管理
 */
@Tag(name = "管理端-标签管理")
@RestController
@RequestMapping("/admin/tags")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @Operation(summary = "标签列表（含禁用）")
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listAll());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    public Result<TagVO> create(@Valid @RequestBody CreateTagDTO dto) {
        return Result.ok(tagService.create(dto));
    }

    @Operation(summary = "编辑标签")
    @PutMapping("/{id}")
    public Result<TagVO> update(@PathVariable Long id,
                                @Valid @RequestBody CreateTagDTO dto) {
        return Result.ok(tagService.update(id, dto));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换标签启用/禁用")
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        tagService.toggleStatus(id);
        return Result.ok();
    }
}
