<template>
  <article class="post-card" @click="goDetail">
    <!-- Author row (top) -->
    <div class="card-author">
      <el-avatar :size="22" :src="post.author?.avatarUrl" class="author-avatar">
        {{ post.author?.nickname?.charAt(0) || 'U' }}
      </el-avatar>
      <span class="author-name">{{ post.author?.nickname }}</span>
      <button
        v-if="showFollowBtn"
        class="follow-chip"
        :class="{ 'is-following': post.authorIsFollowing }"
        :disabled="followLoading"
        @click.stop="toggleFollow"
      >
        {{ followLoading ? '...' : post.authorIsFollowing ? '已关注' : '+ 关注' }}
      </button>
      <span class="card-time">{{ formatRelativeTime(post.createTime) }}</span>
      <span v-if="post.isPinned" class="badge-pin">置顶</span>
      <span v-if="post.isFeatured" class="badge-featured">精华</span>
    </div>

    <!-- Title -->
    <h3 class="card-title" v-html="displayTitle"></h3>

    <!-- Summary -->
    <p class="card-summary" v-if="post.summary || post.contentText">
      {{ post.summary || post.contentText?.slice(0, 180) }}
    </p>

    <!-- Footer: category + stats -->
    <div class="card-footer">
      <div class="footer-left">
        <span v-if="post.category" class="category-tag">
          {{ post.category.icon }} {{ post.category.name }}
        </span>
        <span v-for="tag in post.tags?.slice(0, 3)" :key="tag.id" class="tag-chip">
          {{ tag.name }}
        </span>
      </div>
      <div class="footer-stats">
        <span class="stat"><el-icon :size="14"><View /></el-icon>{{ formatCount(post.viewCount) }}</span>
        <span class="stat"><el-icon :size="14"><ChatDotRound /></el-icon>{{ formatCount(post.commentCount) }}</span>
        <span class="stat stat--like"><el-icon :size="14"><Star /></el-icon>{{ formatCount(post.likeCount) }}</span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PostVO } from '@/types'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { userApi } from '@/api/modules/user'
import { formatRelativeTime, formatCount, highlightText } from '@/utils'
import { View, ChatDotRound, Star } from '@element-plus/icons-vue'

const props = defineProps<{ post: PostVO }>()
const router = useRouter()
const userStore = useUserStore()
const followLoading = ref(false)

const showFollowBtn = computed(() =>
  userStore.isLoggedIn && userStore.userInfo?.id !== props.post.author?.id
)

async function toggleFollow() {
  followLoading.value = true
  try {
    if (props.post.authorIsFollowing) {
      await userApi.unfollowUser(props.post.author.id)
      props.post.authorIsFollowing = false
    } else {
      await userApi.followUser(props.post.author.id)
      props.post.authorIsFollowing = true
    }
  } catch { /* handled */ }
  finally { followLoading.value = false }
}

const displayTitle = computed(() => {
  const title = props.post.title
  if (title.includes('<em>')) {
    return highlightText(title)
  }
  return title
})

function goDetail() { router.push(`/posts/${props.post.id}`) }
</script>

<style lang="scss" scoped>
.post-card {
  padding: var(--spacing-lg) 0;
  cursor: pointer;
  border-bottom: 1px solid var(--color-divider);
  transition: padding var(--transition-fast);

  &:first-child {
    padding-top: 0;
  }

  &:hover {
    .card-title {
      color: var(--color-primary);
    }
  }
}

// ── Author Row ──
.card-author {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.author-avatar {
  flex-shrink: 0;
}

.author-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.follow-chip {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  height: 22px;
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-family: var(--font-family);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    background: var(--color-primary-50);
  }

  &.is-following {
    color: var(--color-text-tertiary);

    &:hover {
      color: var(--color-danger);
      background: #FEF2F2;
    }
  }
}

.card-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin-left: auto;
}

.badge-pin,
.badge-featured {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: var(--font-weight-medium);
  flex-shrink: 0;
}

.badge-pin {
  color: var(--color-danger);
  background: #FEF2F2;
}

.badge-featured {
  color: var(--color-warning);
  background: #FFFBEB;
}

// ── Title ──
.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
  margin-bottom: var(--spacing-sm);
  transition: color var(--transition-fast);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// ── Summary ──
.card-summary {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  line-height: var(--line-height-normal);
  margin-bottom: var(--spacing-md);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// ── Footer ──
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
  flex-wrap: wrap;
}

.category-tag {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-primary-50);
  white-space: nowrap;
  flex-shrink: 0;
}

.tag-chip {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--color-bg-secondary);
  white-space: nowrap;

  &::before {
    content: '#';
    opacity: 0.5;
  }
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-shrink: 0;
}

.stat {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);

  &--like {
    @media (max-width: 767px) {
      display: none;
    }
  }
}

@media (max-width: 767px) {
  .post-card {
    padding: var(--spacing-md) 0;
  }

  .card-title {
    font-size: var(--font-size-md);
  }

  .card-summary {
    -webkit-line-clamp: 2;
  }
}
</style>
