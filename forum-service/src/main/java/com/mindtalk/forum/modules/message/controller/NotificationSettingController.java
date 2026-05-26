package com.mindtalk.forum.modules.message.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.message.service.NotificationSettingService;
import com.mindtalk.forum.modules.message.vo.NotificationSettingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通知设置")
@RestController
@RequestMapping("/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService settingService;

    @Operation(summary = "获取通知设置列表")
    @GetMapping
    public Result<List<NotificationSettingVO>> list() {
        Long userId = getCurrentUserId();
        return Result.ok(settingService.getSettings(userId));
    }

    @Operation(summary = "更新单条通知设置")
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String notifyType = (String) body.get("notifyType");
        boolean enabled = (Boolean) body.get("enabled");
        settingService.updateSetting(userId, notifyType, enabled);
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("未登录");
    }
}
