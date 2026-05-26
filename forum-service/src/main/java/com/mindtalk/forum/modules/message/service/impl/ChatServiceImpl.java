package com.mindtalk.forum.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.message.entity.Conversation;
import com.mindtalk.forum.modules.message.entity.Message;
import com.mindtalk.forum.modules.message.mapper.ConversationMapper;
import com.mindtalk.forum.modules.message.mapper.MessageMapper;
import com.mindtalk.forum.modules.message.service.ChatService;
import com.mindtalk.forum.modules.message.vo.ConversationVO;
import com.mindtalk.forum.modules.message.vo.MessageVO;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<Conversation> conversations = conversationMapper.selectByUserId(userId);
        if (conversations.isEmpty()) return Collections.emptyList();

        Set<Long> otherUserIds = new HashSet<>();
        for (Conversation c : conversations) {
            otherUserIds.add(c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id());
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(new ArrayList<>(otherUserIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return conversations.stream().map(c -> {
            Long otherId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            User other = userMap.get(otherId);
            return ConversationVO.builder()
                    .id(c.getId())
                    .otherUserId(otherId)
                    .otherUsername(other != null ? other.getUsername() : "")
                    .otherNickname(other != null ? other.getNickname() : "未知用户")
                    .otherAvatarUrl(other != null ? other.getAvatarUrl() : null)
                    .lastMessage(c.getLastMessage())
                    .lastMessageAt(c.getLastMessageAt())
                    .unreadCount(0)
                    .createTime(c.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<MessageVO> getMessages(Long userId, Long conversationId, int page, int size) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) return PageResult.of(Collections.emptyList(), 0L, page, size);

        IPage<Message> result = messageMapper.selectByConversationId(new Page<>(page, size), conversationId);

        // Mark messages from the other user as read
        List<Message> messages = result.getRecords();
        for (Message msg : messages) {
            if (!msg.getSenderId().equals(userId) && !Boolean.TRUE.equals(msg.getIsRead())) {
                msg.setIsRead(true);
                msg.setReadAt(LocalDateTime.now());
                messageMapper.updateById(msg);
            }
        }

        List<MessageVO> vos = messages.stream()
                .map(m -> MessageVO.builder()
                        .id(m.getId())
                        .conversationId(m.getConversationId())
                        .senderId(m.getSenderId())
                        .content(m.getContent())
                        .isRead(m.getIsRead())
                        .readAt(m.getReadAt())
                        .createTime(m.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public MessageVO sendMessage(Long senderId, Long conversationId, String content) {
        Message msg = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .isRead(false)
                .build();
        messageMapper.insert(msg);

        // Update conversation's last message
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv != null) {
            conv.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            conv.setLastMessageAt(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }

        log.info("[私信] senderId={} conversationId={}", senderId, conversationId);
        return MessageVO.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .senderId(msg.getSenderId())
                .content(msg.getContent())
                .isRead(msg.getIsRead())
                .createTime(msg.getCreateTime())
                .build();
    }

    @Override
    @Transactional
    public ConversationVO getOrCreateConversation(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new RuntimeException("不能和自己创建会话");
        }

        Conversation existing = conversationMapper.findByUserPair(userId, otherUserId);
        if (existing != null) {
            return getConversations(userId).stream()
                    .filter(c -> c.getId().equals(existing.getId()))
                    .findFirst().orElse(null);
        }

        Long user1Id = Math.min(userId, otherUserId);
        Long user2Id = Math.max(userId, otherUserId);
        Conversation conv = Conversation.builder()
                .user1Id(user1Id)
                .user2Id(user2Id)
                .build();
        conversationMapper.insert(conv);

        User other = userMapper.selectById(otherUserId);
        return ConversationVO.builder()
                .id(conv.getId())
                .otherUserId(otherUserId)
                .otherUsername(other != null ? other.getUsername() : "")
                .otherNickname(other != null ? other.getNickname() : "未知用户")
                .otherAvatarUrl(other != null ? other.getAvatarUrl() : null)
                .unreadCount(0)
                .createTime(conv.getCreateTime())
                .build();
    }

    @Override
    public int getUnreadCount(Long userId) {
        return messageMapper.countUnread(userId);
    }
}
