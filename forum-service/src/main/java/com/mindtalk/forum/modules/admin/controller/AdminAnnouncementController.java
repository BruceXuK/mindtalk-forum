package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.announcement.dto.CreateAnnouncementDTO;
import com.mindtalk.forum.modules.announcement.service.AnnouncementService;
import com.mindtalk.forum.modules.announcement.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-公告管理")
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "公告列表（全部）")
    @GetMapping
    public Result<List<AnnouncementVO>> list() {
        return Result.ok(announcementService.listAll());
    }

    @Operation(summary = "新增公告")
    @PostMapping
    public Result<AnnouncementVO> create(@Valid @RequestBody CreateAnnouncementDTO dto,
                                         @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.ok(announcementService.create(dto, userId != null ? userId : 1L));
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    public Result<AnnouncementVO> update(@PathVariable Long id,
                                         @Valid @RequestBody CreateAnnouncementDTO dto) {
        return Result.ok(announcementService.update(id, dto));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "发布/撤回公告")
    @PutMapping("/{id}/publish")
    public Result<Void> togglePublish(@PathVariable Long id) {
        announcementService.togglePublish(id);
        return Result.ok();
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/{id}/pin")
    public Result<Void> togglePin(@PathVariable Long id) {
        announcementService.togglePin(id);
        return Result.ok();
    }
}
