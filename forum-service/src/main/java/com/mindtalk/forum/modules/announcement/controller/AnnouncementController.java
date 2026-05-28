package com.mindtalk.forum.modules.announcement.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.announcement.service.AnnouncementService;
import com.mindtalk.forum.modules.announcement.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公告")
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "获取有效公告列表")
    @GetMapping("/active")
    public Result<List<AnnouncementVO>> listActive() {
        return Result.ok(announcementService.listActive());
    }
}
