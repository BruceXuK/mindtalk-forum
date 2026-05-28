<template>
  <div class="comment-item" :class="{ 'is-reply': isReply }">
    <el-avatar :size="isReply ? 28 : 36" :src="comment.user?.avatarUrl" class="comment-avatar">
      {{ comment.user?.nickname?.charAt(0) || 'U' }}
    </el-avatar>
    <div class="comment-body">
      <div class="comment-header">
        <span class="comment-author">{{ comment.user?.nickname }}</span>
        <span v-if="comment.replyTo" class="reply-to">
          回复 <span class="reply-target">@{{ comment.replyTo?.nickname }}</span>
        </span>
        <span class="comment-time">{{ formatRelativeTime(comment.createTime) }}</span>
      </div>
      <p class="comment-content">{{ comment.content }}</p>
      <div class="comment-actions">
        <span class="action-btn" :class="{ active: comment.isLiked }" @click="$emit('like', comment.id)">
          <el-icon :size="14"><StarFilled v-if="comment.isLiked" /><Star v-else /></el-icon>
          <span v-if="comment.likeCount">{{ comment.likeCount }}</span>
        </span>
        <span class="action-btn" @click="showReplyInput = !showReplyInput">
          <el-icon :size="14"><ChatDotRound /></el-icon> 回复
        </span>
        <span v-if="canDelete" class="action-btn danger" @click="$emit('delete', comment.id)">
          删除
        </span>
        <span class="action-btn report" @click="showReportDialog = true">
          举报
        </span>
      </div>

      <!-- Inline reply form -->
      <div v-if="showReplyInput" class="inline-reply-form">
        <el-input
          v-model="replyText"
          type="textarea"
          :rows="3"
          :placeholder="`回复 @${comment.user?.nickname}...`"
          resize="none"
          @keydown.enter.ctrl="submitReply"
        />
        <div class="inline-reply-actions">
          <span class="inline-reply-hint">Ctrl + Enter 发送</span>
          <div class="inline-reply-btns">
            <el-button size="small" @click="cancelReply">取消</el-button>
            <el-button type="primary" size="small" :disabled="!replyText.trim()" :loading="submitting" @click="submitReply">
              回复
            </el-button>
          </div>
        </div>
      </div>

      <!-- 子回复列表 -->
      <div v-if="(comment.replies && comment.replies.length > 0) || (comment.replyCount || 0) > 0" class="replies">
        <CommentItem
          v-for="reply in displayReplies"
          :key="reply.id"
          :comment="reply"
          :is-reply="true"
          :can-delete="canDelete"
          @reply="$emit('reply', $event)"
          @delete="$emit('delete', $event)"
          @like="$emit('like', $event)"
          @submit-reply="$emit('submit-reply', $event)"
        />
        <!-- 展开/收起 -->
        <button
          v-if="hasMoreReplies"
          class="expand-replies-btn"
          @click="toggleExpand"
        >
          {{ expanded ? '收起回复' : `展开更多 ${remainingCount} 条回复` }}
        </button>
      </div>
    </div>

    <ReportDialog
      v-model="showReportDialog"
      target-type="COMMENT"
      :target-id="comment.id"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CommentVO } from '@/types'
import { commentApi } from '@/api/modules/comment'
import { formatRelativeTime } from '@/utils'
import { Star, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
import ReportDialog from '@/components/common/ReportDialog.vue'

const props = defineProps<{ comment: CommentVO; isReply?: boolean; canDelete?: boolean }>()
const emit = defineEmits<{
  reply: [comment: CommentVO]
  delete: [commentId: number]
  like: [commentId: number]
  'submit-reply': [data: { parentId: number; replyToId: number; content: string; callback: () => void }]
}>()

const showReplyInput = ref(false)
const showReportDialog = ref(false)
const replyText = ref('')
const submitting = ref(false)

// ── Expand/collapse replies ──
const expanded = ref(false)
const allReplies = ref<CommentVO[]>([])
const loadingReplies = ref(false)

const PREVIEW_LIMIT = 3

const totalReplyCount = computed(() => props.comment.replyCount || 0)
const previewReplies = computed(() => (props.comment.replies || []).slice(0, PREVIEW_LIMIT))
const hasMoreReplies = computed(() => totalReplyCount.value > PREVIEW_LIMIT)
const remainingCount = computed(() => totalReplyCount.value - PREVIEW_LIMIT)
const displayReplies = computed(() => expanded.value ? allReplies.value : previewReplies.value)

async function toggleExpand() {
  if (expanded.value) {
    expanded.value = false
    return
  }
  if (allReplies.value.length > 0) {
    expanded.value = true
    return
  }
  loadingReplies.value = true
  try {
    const res = await commentApi.getReplies(props.comment.id)
    allReplies.value = res.data || []
    expanded.value = true
  } catch { /* ignore */ }
  finally { loadingReplies.value = false }
}

function submitReply() {
  if (!replyText.value.trim()) return
  submitting.value = true
  // parentId 始终指向一级评论，保持所有回复在同一线程内可见
  const rootParentId = props.comment.parentId || props.comment.id
  emit('submit-reply', {
    parentId: rootParentId,
    replyToId: props.comment.user?.id || 0,
    content: replyText.value,
    callback: () => {
      replyText.value = ''
      showReplyInput.value = false
      submitting.value = false
    }
  })
}

function cancelReply() {
  showReplyInput.value = false
  replyText.value = ''
}
</script>

<style lang="scss" scoped>
.comment-item {
  display: flex; gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  border-bottom: var(--border-width) solid var(--color-divider);

  &.is-reply {
    padding: var(--spacing-sm) 0;
    padding-left: var(--spacing-lg);
    border-bottom: none;
    border-left: 2px solid var(--color-divider);
    margin-left: var(--spacing-lg);
  }
}

.comment-avatar { flex-shrink: 0; }

.comment-body { flex: 1; min-width: 0; }

.comment-header {
  display: flex; align-items: center; gap: var(--spacing-sm);
  margin-bottom: 4px; font-size: var(--font-size-sm);

  .comment-author { font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }
  .reply-to { color: var(--color-text-tertiary); }
  .reply-target { color: var(--color-primary); }
  .comment-time { margin-left: auto; color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
}

.comment-content {
  font-size: var(--font-size-base); color: var(--color-text-primary);
  line-height: var(--line-height-normal); margin-bottom: var(--spacing-sm);
  word-break: break-word;
}

.comment-actions {
  display: flex; gap: var(--spacing-md); font-size: var(--font-size-xs);

  .action-btn {
    display: flex; align-items: center; gap: 2px;
    cursor: pointer; color: var(--color-text-tertiary);
    transition: color var(--transition-fast);
    &:hover { color: var(--color-primary); }
    &.active { color: var(--color-like); }
    &.danger:hover { color: var(--color-danger); }
  }
}

// ── Inline Reply Form ──
.inline-reply-form {
  margin-top: var(--spacing-md);
}

.inline-reply-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-sm);
}

.inline-reply-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.inline-reply-btns {
  display: flex;
  gap: var(--spacing-xs);
}

.replies {
  margin-top: var(--spacing-sm);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  padding: 0 var(--spacing-md);
}

.expand-replies-btn {
  display: block;
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-family: var(--font-family);
  cursor: pointer;
  text-align: left;
  transition: opacity var(--transition-fast);

  &:hover { opacity: 0.8; }
}
</style>
