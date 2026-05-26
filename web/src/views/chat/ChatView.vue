<template>
  <div class="chat-page">
    <!-- Left: Conversation List -->
    <div class="chat-sidebar card-base">
      <div class="chat-sidebar__header">
        <h3 class="chat-sidebar__title">私信</h3>
      </div>
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
        <EmptyState
          v-if="conversations.length === 0 && !convLoading"
          title="暂无会话"
          description="去用户主页发起私信吧"
        />
      </div>
    </div>

    <!-- Right: Message Window -->
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
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="msg-bubble"
          :class="{ 'is-self': msg.senderId === userStore.userInfo?.id }"
        >
          <div class="msg-bubble__text">{{ msg.content }}</div>
          <div class="msg-bubble__time">
            {{ formatRelativeTime(msg.createTime) }}
            <span v-if="msg.senderId === userStore.userInfo?.id" class="msg-bubble__status">
              {{ msg.isRead ? '已读' : '未读' }}
            </span>
          </div>
        </div>
        <EmptyState
          v-if="messages.length === 0 && !msgLoading"
          title="暂无消息"
          description="发送第一条消息吧"
        />
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          resize="none"
          @keydown.enter.ctrl="sendMsg"
        />
        <div class="chat-input__footer">
          <span class="chat-input__hint">Ctrl + Enter 发送</span>
          <el-button type="primary" size="small" :loading="sending" :disabled="!inputText.trim()" @click="sendMsg">
            发送
          </el-button>
        </div>
      </div>
    </div>

    <div class="chat-main chat-main--empty card-base" v-else>
      <EmptyState title="选择一个会话" description="从左侧选择或去用户主页发起新会话" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api/modules/chat'
import { useUserStore } from '@/stores/modules/user'
import type { ConversationVO, MessageVO } from '@/types'
import EmptyState from '@/components/common/EmptyState.vue'
import { formatRelativeTime } from '@/utils'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const conversations = ref<ConversationVO[]>([])
const messages = ref<MessageVO[]>([])
const currentConv = ref<ConversationVO | null>(null)
const currentConvId = ref(0)
const inputText = ref('')
const convLoading = ref(false)
const msgLoading = ref(false)
const sending = ref(false)
const msgContainer = ref<HTMLElement>()

onMounted(loadConversations)

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
  messages.value = []
  await loadMessages()
}

async function loadMessages() {
  if (!currentConvId.value) return
  msgLoading.value = true
  try {
    const res = await chatApi.getMessages(currentConvId.value, { page: 1, size: 50 })
    messages.value = res.data.records.reverse()
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
    messages.value.push(res.data)
    inputText.value = ''
    nextTick(() => scrollToBottom())
    loadConversations()
  } catch { /* handled */ }
  finally { sending.value = false }
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  height: calc(100vh - var(--header-height) - var(--spacing-xl) * 2);
  gap: var(--spacing-md);
}

.chat-sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &__header {
    padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
    border-bottom: 1px solid var(--color-divider);
  }

  &__title {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  border-bottom: 1px solid var(--color-divider);
  transition: background var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }
  &.active { background: var(--color-primary-50); }

  &__info { flex: 1; min-width: 0; }

  &__top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
  }

  &__name {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
  }

  &__time {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    flex-shrink: 0;
  }

  &__bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__preview {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 180px;
  }

  &__badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: var(--radius-full);
    background: var(--color-primary);
    color: #fff;
    font-size: 11px;
    font-weight: var(--font-weight-medium);
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &--empty {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__header {
    display: flex;
    align-items: center;
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--color-divider);
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
  padding: var(--spacing-lg);
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

    .msg-bubble__text {
      background: var(--color-primary);
      color: #fff;
      border-radius: var(--radius-lg) 4px var(--radius-lg) var(--radius-lg);
    }

    .msg-bubble__time { text-align: right; }
  }

  &:not(.is-self) .msg-bubble__text {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
    border-radius: 4px var(--radius-lg) var(--radius-lg) var(--radius-lg);
  }

  &__text {
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: var(--font-size-sm);
    line-height: var(--line-height-normal);
    word-break: break-word;
  }

  &__time {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    margin-top: 2px;
    padding: 0 4px;
  }

  &__status {
    margin-left: 4px;
  }
}

.chat-input {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--color-divider);

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: var(--spacing-sm);
  }

  &__hint {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }
}

@media (max-width: 767px) {
  .chat-page { flex-direction: column; height: auto; }
  .chat-sidebar { width: 100%; max-height: 40vh; }
  .chat-main { min-height: 60vh; }
}
</style>
