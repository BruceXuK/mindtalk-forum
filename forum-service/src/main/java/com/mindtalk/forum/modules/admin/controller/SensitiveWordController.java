package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.entity.SensitiveWord;
import com.mindtalk.forum.modules.admin.service.SensitiveWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "敏感词管理")
@RestController
@RequestMapping("/admin/sensitive-words")
@RequiredArgsConstructor
public class SensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    @Operation(summary = "敏感词列表")
    @GetMapping
    public Result<PageResult<SensitiveWord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(sensitiveWordService.getList(page, size));
    }

    @Operation(summary = "添加敏感词")
    @PostMapping
    public Result<SensitiveWord> add(@RequestBody Map<String, String> body) {
        return Result.ok(sensitiveWordService.add(body.get("word"), body.get("replacement")));
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.ok();
    }
}
