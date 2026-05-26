package com.mindtalk.forum.modules.message.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.message.vo.ConversationVO;
import com.mindtalk.forum.modules.message.vo.MessageVO;

import java.util.List;

public interface ChatService {

    List<ConversationVO> getConversations(Long userId);

    PageResult<MessageVO> getMessages(Long userId, Long conversationId, int page, int size);

    MessageVO sendMessage(Long senderId, Long conversationId, String content);

    ConversationVO getOrCreateConversation(Long userId, Long otherUserId);

    int getUnreadCount(Long userId);
}
