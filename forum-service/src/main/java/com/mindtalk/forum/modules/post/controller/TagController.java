package com.mindtalk.forum.modules.post.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreateTagDTO;
import com.mindtalk.forum.modules.post.service.TagService;
import com.mindtalk.forum.modules.post.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签接口
 */
@Tag(name = "标签管理")
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "标签列表")
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.list());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TagVO> create(@Valid @RequestBody CreateTagDTO dto) {
        return Result.ok(tagService.create(dto));
    }

    @Operation(summary = "编辑标签")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TagVO> update(@PathVariable Long id,
                                 @Valid @RequestBody CreateTagDTO dto) {
        return Result.ok(tagService.update(id, dto));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
