package com.mindtalk.forum.modules.message.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.message.service.ChatService;
import com.mindtalk.forum.modules.message.vo.ConversationVO;
import com.mindtalk.forum.modules.message.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "私信聊天")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "获取会话列表")
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> conversations() {
        Long userId = getCurrentUserId();
        return Result.ok(chatService.getConversations(userId));
    }

    @Operation(summary = "获取会话消息")
    @GetMapping("/conversations/{id}/messages")
    public Result<PageResult<MessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(chatService.getMessages(userId, id, page, size));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversations/{id}/messages")
    public Result<MessageVO> sendMessage(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String content = body.get("content");
        return Result.ok(chatService.sendMessage(userId, id, content));
    }

    @Operation(summary = "发起/获取与用户的会话")
    @PostMapping("/start/{userId}")
    public Result<ConversationVO> startConversation(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        return Result.ok(chatService.getOrCreateConversation(currentUserId, userId));
    }

    @Operation(summary = "获取未读私信数")
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount() {
        Long userId = getCurrentUserId();
        return Result.ok(Map.of("count", chatService.getUnreadCount(userId)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("未登录");
    }
}
