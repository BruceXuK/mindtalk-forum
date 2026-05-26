import request from '../request'
import type { Result, PageResult, ConversationVO, MessageVO } from '@/types'

export const chatApi = {
  getConversations() {
    return request.get<any, Result<ConversationVO[]>>('/chat/conversations')
  },

  getMessages(conversationId: number, params?: { page?: number; size?: number }) {
    return request.get<any, Result<PageResult<MessageVO>>>(`/chat/conversations/${conversationId}/messages`, { params })
  },

  sendMessage(conversationId: number, content: string) {
    return request.post<any, Result<MessageVO>>(`/chat/conversations/${conversationId}/messages`, { content })
  },

  startConversation(userId: number) {
    return request.post<any, Result<ConversationVO>>(`/chat/start/${userId}`)
  },

  getUnreadCount() {
    return request.get<any, Result<{ count: number }>>('/chat/unread-count')
  }
}
