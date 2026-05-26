<template>
  <div class="message-page">
    <div class="message-header">
      <div class="message-tabs">
        <button class="msg-tab" :class="{ active: msgTab === 'notifications' }" @click="msgTab = 'notifications'">通知</button>
        <button class="msg-tab" :class="{ active: msgTab === 'chat' }" @click="msgTab = 'chat'; loadConversations()">私信</button>
      </div>
      <el-button v-if="msgTab === 'notifications' && messages.some(m => !m.isRead)" text size="small" @click="markAllRead">全部已读</el-button>
    </div>

    <!-- 通知列表 -->
    <template v-if="msgTab === 'notifications'">
    <div class="message-list card-base" v-loading="loading">
      <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ unread: !msg.isRead }" @click="navigateTo(msg)">
        <el-avatar :size="40" :src="msg.fromUser?.avatarUrl">
          {{ msg.fromUser?.nickname?.charAt(0) || 'S' }}
        </el-avatar>
        <div class="message-body">
          <div class="message-top">
            <span class="msg-title">
              <span class="msg-type-tag" :class="'msg-type--' + msg.notifyType.toLowerCase()">
                {{ notifyTypeLabel(msg.notifyType) }}
              </span>
              {{ msg.title }}
            </span>
            <span class="msg-time">{{ formatRelativeTime(msg.createTime) }}</span>
          </div>
          <p class="msg-content">{{ msg.content }}</p>
        </div>
        <span v-if="!msg.isRead" class="unread-dot"></span>
      </div>
      <EmptyState v-if="messages.length === 0 && !loading" title="暂无消息" description="你已查看所有消息" />
    </div>
    </template>

    <!-- 私信 -->
    <div class="chat-wrap" v-if="msgTab === 'chat'">
      <div class="chat-sidebar card-base">
        <div class="conversation-list" v-loading="convLoading">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: currentConvId === conv.id }"
            @click="openConversation(conv)"
          >
            <el-avatar :size="44" :src="conv.otherAvatarUrl">
              {{ conv.otherNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="conv-item__info">
              <div class="conv-item__top">
                <span class="conv-item__name">{{ conv.otherNickname }}</span>
                <span class="conv-item__time" v-if="conv.lastMessageAt">{{ formatRelativeTime(conv.lastMessageAt) }}</span>
              </div>
              <div class="conv-item__bottom">
                <span class="conv-item__preview">{{ conv.lastMessage || '暂无消息' }}</span>
                <span v-if="conv.unreadCount > 0" class="conv-item__badge">{{ conv.unreadCount }}</span>
              </div>
            </div>
          </div>
          <EmptyState v-if="conversations.length === 0 && !convLoading" title="暂无会话" description="去用户主页发起私信吧" />
        </div>
      </div>

      <div class="chat-main card-base" v-if="currentConv">
        <div class="chat-main__header">
          <router-link :to="`/users/${currentConv.otherUserId}`" class="chat-main__user">
            <el-avatar :size="36" :src="currentConv.otherAvatarUrl">
              {{ currentConv.otherNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span>{{ currentConv.otherNickname }}</span>
          </router-link>
        </div>
        <div class="chat-messages" ref="msgContainer" v-loading="msgLoading">
          <div v-for="msg in chatMessages" :key="msg.id" class="msg-bubble" :class="{ 'is-self': msg.senderId === userStore.userInfo?.id }">
            <div class="msg-bubble__text">{{ msg.content }}</div>
            <div class="msg-bubble__time">
              {{ formatRelativeTime(msg.createTime) }}
              <span v-if="msg.senderId === userStore.userInfo?.id" class="msg-bubble__status">{{ msg.isRead ? '已读' : '未读' }}</span>
            </div>
          </div>
          <EmptyState v-if="chatMessages.length === 0 && !msgLoading" title="暂无消息" description="发送第一条消息吧" />
        </div>
        <div class="chat-input">
          <el-input v-model="inputText" type="textarea" :rows="2" placeholder="输入消息..." resize="none" @keydown.enter.ctrl="sendMsg" />
          <div class="chat-input__footer">
            <span class="chat-input__hint">Ctrl + Enter 发送</span>
            <el-button type="primary" size="small" :loading="sending" :disabled="!inputText.trim()" @click="sendMsg">发送</el-button>
          </div>
        </div>
      </div>

      <div class="chat-main chat-main--empty card-base" v-else-if="!convLoading">
        <EmptyState title="选择一个会话" description="从左侧选择或去用户主页发起新会话" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '@/api/modules/message'
import { chatApi } from '@/api/modules/chat'
import { useUserStore } from '@/stores/modules/user'
import type { NotificationVO, ConversationVO, MessageVO } from '@/types'
import EmptyState from '@/components/common/EmptyState.vue'
import { formatRelativeTime } from '@/utils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const messages = ref<NotificationVO[]>([])
const msgTab = ref<'notifications' | 'chat'>('notifications')
const loading = ref(false)

onMounted(loadMessages)

async function loadMessages() {
  loading.value = true
  try {
    const res = await messageApi.getList({ page: 1, size: 50 })
    messages.value = res.data.records
  } finally { loading.value = false }
}

function navigateTo(msg: NotificationVO) {
  if (msg.targetType === 'POST' || msg.targetType === 'COMMENT') {
    router.push(`/posts/${msg.targetId}`)
  } else if (msg.targetType === 'USER') {
    router.push(`/users/${msg.targetId}`)
  }
  if (!msg.isRead) {
    messageApi.markAsRead(msg.id).catch(() => {})
    msg.isRead = true
  }
}

async function markAllRead() {
  try { await messageApi.markAllAsRead(); loadMessages() } catch { /* ignore */ }
}

function notifyTypeLabel(type: string): string {
  const map: Record<string, string> = {
    LIKE: '赞', COMMENT: '评论', FOLLOW: '关注', MENTION: '@提及', SYSTEM: '系统'
  }
  return map[type] || type
}

// ── 私信 ──
const conversations = ref<ConversationVO[]>([])
const chatMessages = ref<MessageVO[]>([])
const currentConv = ref<ConversationVO | null>(null)
const currentConvId = ref(0)
const inputText = ref('')
const convLoading = ref(false)
const msgLoading = ref(false)
const sending = ref(false)
const msgContainer = ref<HTMLElement>()

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await chatApi.getConversations()
    conversations.value = res.data
  } catch { /* handled */ }
  finally { convLoading.value = false }
}

async function openConversation(conv: ConversationVO) {
  currentConv.value = conv
  currentConvId.value = conv.id
  chatMessages.value = []
  await loadChatMessages()
}

async function loadChatMessages() {
  if (!currentConvId.value) return
  msgLoading.value = true
  try {
    const res = await chatApi.getMessages(currentConvId.value, { page: 1, size: 50 })
    chatMessages.value = res.data.records.reverse()
    nextTick(() => scrollToBottom())
  } catch { /* handled */ }
  finally { msgLoading.value = false }
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

async function sendMsg() {
  if (!inputText.value.trim() || !currentConvId.value) return
  sending.value = true
  try {
    const res = await chatApi.sendMessage(currentConvId.value, inputText.value)
    chatMessages.value.push(res.data)
    inputText.value = ''
    nextTick(() => scrollToBottom())
    loadConversations()
  } catch { /* handled */ }
  finally { sending.value = false }
}
</script>

<style lang="scss" scoped>
.message-page { width: 100%; }

.message-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-divider);
}

.message-tabs { display: flex; gap: var(--spacing-xs); }

.msg-tab {
  padding: 6px 18px;
  border: none;
  background: none;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover:not(.active) { background: var(--color-bg-secondary); }

  &.active {
    background: var(--color-text-primary);
    color: #fff;
  }
}

// ── 通知列表 ──
.message-list { padding: 0; }

.message-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-sm);
  position: relative;
  cursor: pointer;
  transition: background var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }

  &.unread {
    background: var(--color-primary-50);
    &:hover { background: #EFF6FF; }
  }
}

.message-body { flex: 1; min-width: 0; }
.message-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.msg-title { font-weight: var(--font-weight-semibold); font-size: var(--font-size-base); color: var(--color-text-primary); }
.msg-time { font-size: var(--font-size-xs); color: var(--color-text-tertiary); flex-shrink: 0; margin-left: var(--spacing-sm); }
.msg-content { font-size: var(--font-size-sm); color: var(--color-text-secondary); line-height: var(--line-height-normal); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 400px; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary); flex-shrink: 0; margin-top: 8px; }

.msg-type-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: var(--font-weight-medium);
  margin-right: 6px;
  vertical-align: middle;

  &.msg-type--like { background: #FEF2F2; color: #EF4444; }
  &.msg-type--comment { background: #EFF6FF; color: #3B82F6; }
  &.msg-type--follow { background: #F0FDF4; color: #22C55E; }
  &.msg-type--mention { background: #FFF7ED; color: #F97316; }
  &.msg-type--system { background: #F8FAFC; color: #64748B; }
}

// ── 私信 ──
.chat-wrap { display: flex; gap: var(--spacing-md); height: calc(100vh - var(--header-height) - 120px); }

.chat-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conversation-list { flex: 1; overflow-y: auto; }

.conv-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  cursor: pointer;
  border-bottom: 1px solid var(--color-divider);
  transition: background var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }
  &.active { background: var(--color-primary-50); }

  &__info { flex: 1; min-width: 0; }
  &__top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px; }
  &__name { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
  &__time { font-size: var(--font-size-xs); color: var(--color-text-tertiary); flex-shrink: 0; }
  &__bottom { display: flex; justify-content: space-between; align-items: center; }
  &__preview { font-size: var(--font-size-xs); color: var(--color-text-tertiary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }
  &__badge { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 5px; border-radius: var(--radius-full); background: var(--color-primary); color: #fff; font-size: 11px; font-weight: var(--font-weight-medium); }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &--empty { align-items: center; justify-content: center; }

  &__header {
    display: flex;
    align-items: center;
    padding: var(--spacing-sm) var(--spacing-md);
    border-bottom: 1px solid var(--color-divider);
    flex-shrink: 0;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    &:hover { color: var(--color-primary); }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-md);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.msg-bubble {
  display: flex;
  flex-direction: column;
  max-width: 70%;

  &.is-self {
    align-self: flex-end;
    .msg-bubble__text { background: var(--color-primary); color: #fff; border-radius: var(--radius-lg) 4px var(--radius-lg) var(--radius-lg); }
    .msg-bubble__time { text-align: right; }
  }

  &:not(.is-self) .msg-bubble__text {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
    border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
  }

  &__text { padding: var(--spacing-sm) var(--spacing-md); font-size: var(--font-size-sm); line-height: var(--line-height-normal); word-break: break-word; }
  &__time { font-size: var(--font-size-xs); color: var(--color-text-tertiary); margin-top: 2px; padding: 0 4px; }
  &__status { margin-left: 4px; }
}

.chat-input {
  padding: var(--spacing-sm) var(--spacing-md);
  border-top: 1px solid var(--color-divider);
  flex-shrink: 0;

  &__footer { display: flex; align-items: center; justify-content: space-between; margin-top: var(--spacing-xs); }
  &__hint { font-size: var(--font-size-xs); color: var(--color-text-tertiary); }
}

@media (max-width: 767px) {
  .chat-wrap { flex-direction: column; height: auto; }
  .chat-sidebar { width: 100%; max-height: 30vh; }
  .chat-main { min-height: 50vh; }
}
</style>
