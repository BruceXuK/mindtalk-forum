<template>
  <div class="post-detail" v-loading="loading">
    <!-- TOC Sidebar -->
    <aside v-if="tocItems.length > 0" class="toc-sidebar">
      <nav class="toc-nav">
        <span class="toc-title">目录</span>
        <a
          v-for="item in tocItems"
          :key="item.id"
          class="toc-link"
          :class="{
            'is-active': activeTocId === item.id,
            [`toc-level-${item.level}`]: true
          }"
          :style="{ paddingLeft: (item.level - 1) * 16 + 12 + 'px' }"
          @click.prevent="scrollToHeading(item.id)"
        >
          {{ item.text }}
        </a>
      </nav>
    </aside>

    <!-- Main Content -->
    <main class="detail-main" v-if="post">
      <article class="detail-card card-base">
        <!-- Badges + View Count -->
        <div class="detail-badges">
          <div class="detail-badges__left">
            <span v-if="post.isPinned" class="badge badge-pin">
              <el-icon><Top /></el-icon> 置顶
              <span v-if="post.pinnedUntil" class="badge-expiry">至 {{ formatDate(post.pinnedUntil) }}</span>
            </span>
            <span v-if="post.isFeatured" class="badge badge-featured">
              <el-icon><StarFilled /></el-icon> 精华
              <span v-if="post.featuredUntil" class="badge-expiry">至 {{ formatDate(post.featuredUntil) }}</span>
            </span>
          </div>
          <span class="view-count-badge" v-if="post.viewCount">
            <el-icon><View /></el-icon>
            <span>{{ formatCount(post.viewCount) }} 阅读</span>
          </span>
        </div>

        <!-- Title -->
        <h1 class="detail-title">{{ post.title }}</h1>

        <!-- Author Row -->
        <div class="author-row">
          <div class="author-row__left">
            <router-link :to="`/users/${post.author?.id}`" class="author-link">
              <el-avatar :size="44" :src="post.author?.avatarUrl" class="author-avatar">
                {{ post.author?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="author-meta">
                <span class="author-name">{{ post.author?.nickname }}</span>
                <span class="author-sub">
                  <span>{{ formatTime(post.createTime) }} 发布</span>
                  <span v-if="post.updateTime !== post.createTime" class="edited-mark">
                    · 编辑于 {{ formatTime(post.updateTime) }}
                  </span>
                </span>
              </div>
            </router-link>
            <el-button
              v-if="isLoggedIn && !isAuthor && !post.authorIsFollowing"
              type="primary"
              size="small"
              round
              :loading="followLoading"
              @click.stop="handleFollowAuthor"
            >
              + 关注
            </el-button>
            <el-button
              v-if="isLoggedIn && !isAuthor && post.authorIsFollowing"
              size="small"
              round
              :loading="followLoading"
              @click.stop="handleUnfollowAuthor"
            >
              已关注
            </el-button>
          </div>

          <div class="author-row__actions">
            <button
              class="action-btn action-btn--like"
              :class="{ 'is-active': post.isLiked }"
              @click="handleLike"
              v-if="isLoggedIn"
            >
              <el-icon :size="18">
                <StarFilled v-if="post.isLiked" />
                <Star v-else />
              </el-icon>
              <span>{{ formatCount(post.likeCount) }}</span>
            </button>
            <button
              class="action-btn"
              :class="{ 'is-active': post.isCollected }"
              @click="handleCollect"
              v-if="isLoggedIn"
            >
              <el-icon :size="18">
                <FolderChecked v-if="post.isCollected" />
                <FolderAdd v-else />
              </el-icon>
              <span>{{ post.isCollected ? '已收藏' : '收藏' }}</span>
            </button>
            <button class="action-btn" @click="handleShare">
              <el-icon :size="16"><Share /></el-icon>
              <span>分享</span>
            </button>
          </div>
        </div>

        <!-- Content -->
        <div
          ref="contentRef"
          class="detail-body markdown-body"
          v-html="renderedHtml"
          @click="handleContentClick"
        ></div>

        <!-- Category & Tags -->
        <div class="detail-meta-bottom">
          <router-link
            v-if="post.category"
            :to="`/posts?categoryId=${post.category.id}`"
            class="category-link"
          >
            <el-icon><Folder /></el-icon>
            {{ post.category.icon }} {{ post.category.name }}
          </router-link>
          <div class="tag-list" v-if="post.tags?.length">
            <router-link
              v-for="tag in post.tags"
              :key="tag.id"
              :to="`/posts?tagId=${tag.id}`"
              class="tag-chip"
            >
              #{{ tag.name }}
            </router-link>
          </div>
        </div>

        <!-- Bottom Bar -->
        <div class="bottom-bar">
          <div class="bottom-bar__left">
            <button class="action-btn" @click="scrollToComment">
              <el-icon :size="16"><ChatDotRound /></el-icon>
              <span>{{ post.commentCount ? `${formatCount(post.commentCount)} 条评论` : '评论' }}</span>
            </button>
            <button
              v-if="isLoggedIn"
              class="action-btn"
              :class="{ 'is-active': isReadLater }"
              @click="handleReadLater"
            >
              <el-icon :size="16"><Reading /></el-icon>
              <span>{{ isReadLater ? '已标记' : '稍后阅读' }}</span>
            </button>
          </div>

          <div class="bottom-bar__right">
            <button
              v-if="isLoggedIn"
              class="action-btn action-btn--muted"
              @click="showReportDialog = true"
            >
              <el-icon :size="14"><WarningFilled /></el-icon>
              <span>举报</span>
            </button>
            <template v-if="canEdit">
              <button class="action-btn" @click="$router.push(`/posts/${post.id}/edit`)">
                <el-icon :size="14"><EditPen /></el-icon>
                <span>编辑</span>
              </button>
              <el-popconfirm title="确定删除该帖子？" @confirm="handleDelete">
                <template #reference>
                  <button class="action-btn danger">
                    <el-icon :size="14"><Delete /></el-icon>
                    <span>删除</span>
                  </button>
                </template>
              </el-popconfirm>
            </template>
          </div>
        </div>
      </article>

      <!-- Series Navigation -->
      <div v-if="seriesContext" class="series-nav card-base">
        <div class="series-nav__header">
          <el-icon><Collection /></el-icon>
          <span>系列：</span>
          <router-link :to="`/series/${seriesContext.series.id}`" class="series-nav__name">
            {{ seriesContext.series.title }}
          </router-link>
        </div>
        <div class="series-nav__posts">
          <router-link
            v-if="seriesContext.prevPost"
            :to="`/posts/${seriesContext.prevPost.id}`"
            class="series-nav__link prev"
          >
            <el-icon><ArrowLeft /></el-icon>
            <span class="series-nav__label">上一篇</span>
            <span class="series-nav__post-title">{{ seriesContext.prevPost.title }}</span>
          </router-link>
          <span v-else class="series-nav__link disabled">
            <span class="series-nav__label">已经是第一篇</span>
          </span>
          <router-link
            v-if="seriesContext.nextPost"
            :to="`/posts/${seriesContext.nextPost.id}`"
            class="series-nav__link next"
          >
            <span class="series-nav__label">下一篇</span>
            <span class="series-nav__post-title">{{ seriesContext.nextPost.title }}</span>
            <el-icon><ArrowRight /></el-icon>
          </router-link>
          <span v-else class="series-nav__link disabled">
            <span class="series-nav__label">已经是最后一篇</span>
          </span>
        </div>
      </div>

      <!-- Related Posts -->
      <section v-if="relatedPosts.length > 0" class="related-section">
        <div class="section-header">
          <h3 class="section-title">相关推荐</h3>
          <button class="refresh-btn" title="换一换" @click="shuffleRelated">
            <el-icon><Refresh /></el-icon>
          </button>
        </div>
        <div class="related-grid">
          <router-link
            v-for="rp in relatedPosts"
            :key="rp.id"
            :to="`/posts/${rp.id}`"
            class="related-card card-base hover-lift"
          >
            <h4 class="related-card__title">{{ rp.title }}</h4>
            <p class="related-card__summary">{{ rp.summary || rp.contentText?.slice(0, 100) }}</p>
            <div class="related-card__meta">
              <span>{{ rp.author?.nickname }}</span>
              <span>{{ formatCount(rp.viewCount) }} 阅读</span>
            </div>
          </router-link>
        </div>
      </section>

      <!-- Comments -->
      <section class="comments-section card-base" id="comments">
        <h3 class="section-title">
          评论
          <span class="comment-count" v-if="post.commentCount">({{ post.commentCount }})</span>
        </h3>

        <!-- Comment Sort -->
        <div class="comment-sort" v-if="comments.length > 0 || commentTotal > 0">
          <button
            class="sort-btn"
            :class="{ active: commentSort === 'create_time' }"
            @click="commentSort = 'create_time'; reloadComments()"
          >最新</button>
          <button
            class="sort-btn"
            :class="{ active: commentSort === 'like_count' }"
            @click="commentSort = 'like_count'; reloadComments()"
          >最热</button>
        </div>

        <!-- Comment Form (logged in) -->
        <div v-if="isLoggedIn" class="comment-form">
          <div class="comment-form__input-wrap">
            <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl" class="comment-form__avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="comment-form__input-area">
              <el-input
                v-model="commentText"
                type="textarea"
                :rows="3"
                placeholder="写下你的想法... 输入 @ 提及用户"
                resize="none"
                @keydown.enter.ctrl="submitComment"
                @input="onCommentInput"
                @keydown="onCommentKeydown"
              />
              <div class="comment-form__footer">
                <span class="comment-form__hint">Ctrl + Enter 发送</span>
                <el-button
                  type="primary"
                  size="small"
                  :loading="commenting"
                  :disabled="!commentText.trim()"
                  @click="submitComment"
                >
                  发表评论
                </el-button>
              </div>

              <!-- @提及用户建议面板 -->
              <Transition name="fade">
                <div v-if="mentionVisible" class="mention-panel" :style="{ top: mentionPos.top + 'px', left: mentionPos.left + 'px' }">
                  <div v-if="mentionLoading" class="mention-panel__loading">搜索中...</div>
                  <div v-else-if="mentionUsers.length === 0" class="mention-panel__empty">未找到用户</div>
                  <button
                    v-for="user in mentionUsers"
                    :key="user.id"
                    class="mention-item"
                    @click="onSelectCommentMention(user)"
                  >
                    <el-avatar :size="32" :src="user.avatarUrl">{{ user.nickname?.charAt(0) }}</el-avatar>
                    <div class="mention-item__info">
                      <span class="mention-item__name">{{ user.nickname }}</span>
                      <span class="mention-item__username">@{{ user.username }}</span>
                    </div>
                  </button>
                </div>
              </Transition>
            </div>
          </div>
        </div>

        <!-- Comment CTA (not logged in) -->
        <div v-else class="comment-login-cta" @click="promptLogin">
          <el-avatar :size="36" :icon="User" class="comment-login-cta__avatar" />
          <div class="comment-login-cta__input">
            <span>写下你的想法...</span>
          </div>
          <el-button type="primary" size="small" round @click.stop="goLogin">登录</el-button>
        </div>

        <!-- Comment List (Virtual Scroll) -->
        <div v-if="comments.length > 0" ref="commentScrollRef" class="comment-list-virtual">
          <div :style="{ height: virtualizer.getTotalSize() + 'px', width: '100%', position: 'relative' }">
            <div
              v-for="virtualRow in virtualizer.getVirtualItems()"
              :key="virtualRow.index"
              :style="{
                position: 'absolute', top: 0, left: 0, width: '100%',
                transform: `translateY(${virtualRow.start}px)`
              }"
              :data-index="virtualRow.index"
            >
              <CommentItem
                :comment="comments[virtualRow.index]"
                :can-delete="isAdmin"
                @delete="handleDeleteComment"
                @like="handleLikeComment"
                @submit-reply="handleSubmitReply"
              />
            </div>
          </div>
        </div>
        <EmptyState
          v-else-if="!commentLoading"
          title="暂无评论"
          description="来写下第一条评论吧"
          :icon="ChatDotRound"
        />

        <div v-if="commentTotal > comments.length" class="comment-more">
          <el-button :loading="commentLoading" @click="loadMoreComments">
            加载更多评论
          </el-button>
        </div>
      </section>
    </main>

    <!-- NotFound -->
    <EmptyState
      v-else-if="!loading"
      title="帖子不存在"
      description="该帖子可能已被删除或不存在"
    />

    <!-- Image Preview Dialog -->
    <Transition name="fade">
      <div v-if="previewImageUrl" class="image-overlay" @click.self="closeImagePreview">
        <button class="image-overlay__close" @click="closeImagePreview">
          <el-icon :size="28"><Close /></el-icon>
        </button>
        <img :src="previewImageUrl" class="image-overlay__img" alt="预览" />
      </div>
    </Transition>

    <ReportDialog
      v-if="post"
      v-model="showReportDialog"
      target-type="POST"
      :target-id="post.id"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postApi } from '@/api/modules/post'
import { commentApi } from '@/api/modules/comment'
import { userApi } from '@/api/modules/user'
import { seriesApi } from '@/api/modules/series'
import { readingHistoryApi } from '@/api/modules/readingHistory'
import { readLaterApi } from '@/api/modules/readLater'
import { useUserStore } from '@/stores/modules/user'
import type { PostVO, CommentVO, PostSeriesContextVO } from '@/types'
import CommentItem from '@/components/comment/CommentItem.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ReportDialog from '@/components/common/ReportDialog.vue'
import { formatTime, formatRelativeTime, formatCount } from '@/utils'

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}
import { useVirtualizer } from '@tanstack/vue-virtual'
import { renderMarkdown, extractToc, slugify, renderMermaidDiagrams } from '@/composables/useMarkdown'
import { useMention, parseMentionedUsernames } from '@/composables/useMention'
import type { UserVO } from '@/types'
import {
  View, ChatDotRound, Star, StarFilled, EditPen, Delete,
  Top, Folder, FolderChecked, FolderAdd, Close,
  User, Collection, ArrowLeft, ArrowRight, Share, Reading, WarningFilled, Refresh,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const contentRef = ref<HTMLElement>()

const post = ref<PostVO | null>(null)
const comments = ref<CommentVO[]>([])
const allSimilarPosts = ref<PostVO[]>([])
const relatedPosts = ref<PostVO[]>([])
const seriesContext = ref<PostSeriesContextVO | null>(null)
const loading = ref(false)
const commentLoading = ref(false)
const commenting = ref(false)
const commentText = ref('')
const postId = computed(() => Number(route.params.id))
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSort = ref<'create_time' | 'like_count'>('create_time')
const showReportDialog = ref(false)
const commentScrollRef = ref<HTMLElement>()
const virtualizer = useVirtualizer(computed(() => ({
  count: comments.value.length,
  getScrollElement: () => commentScrollRef.value ?? null,
  estimateSize: () => 140,
  overscan: 5
})))
const previewImageUrl = ref('')

// ── @Mention ──
const commentTextareaRef = ref<HTMLTextAreaElement>()
const {
  mentionVisible, mentionUsers, mentionLoading, mentionPos,
  onInput: onCommentMentionInput, selectUser: applyCommentMention, closeMention: closeCommentMention
} = useMention()
const commentMentionedUserIds = ref<number[]>([])

function onCommentInput(e: Event) {
  onCommentMentionInput(e)
}

function onCommentKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeCommentMention()
  }
}

function onSelectCommentMention(user: UserVO) {
  const el = document.querySelector('.comment-form__input-area textarea') as HTMLTextAreaElement
  if (!el) return
  const id = applyCommentMention(user, el)
  commentText.value = el.value
  if (!commentMentionedUserIds.value.includes(id)) {
    commentMentionedUserIds.value.push(id)
  }
}

const isLoggedIn = computed(() => userStore.isLoggedIn)
const isAdmin = computed(() => userStore.hasRole('ADMIN'))
const isAuthor = computed(() => post.value && userStore.userInfo?.id === post.value.author?.id)
const canEdit = computed(() => isAuthor.value)

const contentMarkdown = computed(() => {
  if (!post.value) return ''
  return post.value.contentText || post.value.content || ''
})

const renderedHtml = computed(() => renderMarkdown(contentMarkdown.value))
const tocItems = computed(() => extractToc(contentMarkdown.value))

// ── TOC active tracking ──
const activeTocId = ref('')
let tocObserver: IntersectionObserver | null = null

function setupTocObserver() {
  if (tocItems.value.length === 0) return
  cleanupTocObserver()

  tocObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries.filter(e => e.isIntersecting)
      if (visible.length > 0) {
        activeTocId.value = visible[0].target.id
      }
    },
    { rootMargin: '-80px 0px -70% 0px', threshold: 0 }
  )

  // Wait for render
  nextTick(() => {
    tocItems.value.forEach(item => {
      const el = document.getElementById(item.id)
      if (el) tocObserver?.observe(el)
    })
  })
}

function cleanupTocObserver() {
  tocObserver?.disconnect()
  tocObserver = null
}

watch(tocItems, () => {
  nextTick(setupTocObserver)
})

watch(renderedHtml, () => {
  nextTick(() => {
    if (contentRef.value) {
      renderMermaidDiagrams(contentRef.value)
    }
  })
})

watch(() => post.value, () => {
  nextTick(setupTocObserver)
})

// ── Scroll to heading ──
function scrollToHeading(id: string) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeTocId.value = id
  }
}

// ── Content click handler (code copy + image preview) ──
function handleContentClick(e: MouseEvent) {
  const target = e.target as HTMLElement

  // Code copy button
  const copyBtn = target.closest('.code-copy-btn') as HTMLElement | null
  if (copyBtn) {
    const encoded = copyBtn.getAttribute('data-code')
    if (encoded) {
      const code = decodeURIComponent(encoded)
      navigator.clipboard.writeText(code).then(() => {
        const original = copyBtn.innerHTML
        copyBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        copyBtn.classList.add('copied')
        setTimeout(() => {
          copyBtn.innerHTML = original
          copyBtn.classList.remove('copied')
        }, 2000)
      }).catch(() => { /* ignore */ })
    }
    return
  }

  // Image preview
  const img = target.closest('.content-image') as HTMLImageElement | null
  if (img) {
    previewImageUrl.value = img.src
  }
}

function closeImagePreview() {
  previewImageUrl.value = ''
}

// ── Follow Author ──
const followLoading = ref(false)

async function handleFollowAuthor() {
  if (!post.value) return
  followLoading.value = true
  try {
    await userApi.followUser(post.value.author.id)
    post.value.authorIsFollowing = true
    ElMessage.success('已关注')
  } catch { /* handled */ }
  finally { followLoading.value = false }
}

async function handleUnfollowAuthor() {
  if (!post.value) return
  followLoading.value = true
  try {
    await userApi.unfollowUser(post.value.author.id)
    post.value.authorIsFollowing = false
    ElMessage.success('已取消关注')
  } catch { /* handled */ }
  finally { followLoading.value = false }
}

// ── Load Data ──
onMounted(() => {
  loadPost()
  loadComments()
  recordView()
})

async function loadPost() {
  loading.value = true
  try {
    const res = await postApi.getDetail(postId.value)
    post.value = res.data
    // Load similar posts (fetch 10, shuffle, pick 2)
    postApi.getSimilar(postId.value, 10).then(r => {
      allSimilarPosts.value = r.data || []
      shuffleRelated()
    }).catch(() => {})
    // Load series context
    seriesApi.getPostSeriesContext(postId.value).then(r => {
      seriesContext.value = r.data
    }).catch(() => {})
    // Check read later status
    checkReadLaterStatus()
    await nextTick()
    setupTocObserver()
  } catch {
    /* handled */
  } finally {
    loading.value = false
  }
}

function shuffleRelated() {
  const pool = [...allSimilarPosts.value]
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]]
  }
  relatedPosts.value = pool.slice(0, 2)
}

async function loadComments() {
  commentLoading.value = true
  try {
    const res = await commentApi.getList({
      postId: postId.value,
      page: commentPage.value,
      size: 20,
      orderBy: commentSort.value
    })
    if (commentPage.value === 1) {
      comments.value = res.data.records
    } else {
      comments.value.push(...res.data.records)
    }
    commentTotal.value = res.data.total
  } catch {
    /* handled */
  } finally {
    commentLoading.value = false
  }
}

async function loadMoreComments() {
  commentPage.value++
  await loadComments()
}

function reloadComments() {
  commentPage.value = 1
  loadComments()
}

const isReadLater = ref(false)

function recordView() {
  postApi.recordView(postId.value).catch(() => {})
  if (isLoggedIn.value) {
    readingHistoryApi.record(postId.value).catch(() => {})
  }
}

async function checkReadLaterStatus() {
  if (!isLoggedIn.value) return
  try {
    const res = await readLaterApi.checkStatus(postId.value)
    isReadLater.value = res.data.bookmarked
  } catch { /* ignore */ }
}

async function handleReadLater() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录')
    return
  }
  try {
    if (isReadLater.value) {
      await readLaterApi.remove(postId.value)
      isReadLater.value = false
      ElMessage.success('已从稍后阅读移除')
    } else {
      await readLaterApi.add(postId.value)
      isReadLater.value = true
      ElMessage.success('已添加到稍后阅读')
    }
  } catch { /* handled */ }
}

// ── Actions ──
async function handleLike() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录')
    return
  }
  try {
    await postApi.likePost(postId.value)
    loadPost()
  } catch { /* handled */ }
}

function promptLogin() {
  ElMessage.info('请先登录后再评论')
}

function goLogin() {
  router.push(`/login?redirect=/posts/${postId.value}`)
}

async function handleCollect() {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录')
    return
  }
  try {
    await postApi.collectPost(postId.value)
    loadPost()
  } catch { /* handled */ }
}

async function handleDelete() {
  try {
    await postApi.delete(postId.value)
    ElMessage.success('已删除')
    router.push('/')
  } catch { /* handled */ }
}

function handleShare() {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制链接')
  })
}

async function submitComment() {
  if (!commentText.value.trim()) return
  commenting.value = true
  try {
    await commentApi.create({
      postId: postId.value,
      content: commentText.value,
      mentionedUserIds: commentMentionedUserIds.value,
    })
    commentText.value = ''
    commentMentionedUserIds.value = []
    ElMessage.success('评论成功')
    userStore.fetchUnreadCount()
    commentPage.value = 1
    await loadComments()
    if (post.value) post.value.commentCount++
  } catch { /* handled */ }
  finally { commenting.value = false }
}

async function handleSubmitReply(data: { parentId: number; replyToId: number; content: string; callback: () => void }) {
  if (!isLoggedIn.value) {
    ElMessage.info('请先登录后再评论')
    return
  }
  try {
    await commentApi.create({
      postId: postId.value,
      content: data.content,
      parentId: data.parentId,
      replyToId: data.replyToId
    })
    ElMessage.success('回复成功')
    userStore.fetchUnreadCount()
    commentPage.value = 1
    await loadComments()
    if (post.value) post.value.commentCount++
    data.callback()
  } catch {
    data.callback()
  }
}

async function handleDeleteComment(commentId: number) {
  try {
    await commentApi.delete(commentId)
    ElMessage.success('已删除')
    commentPage.value = 1
    await loadComments()
    if (post.value && post.value.commentCount > 0) post.value.commentCount--
  } catch { /* handled */ }
}

async function handleLikeComment(commentId: number) {
  try {
    await commentApi.like(commentId)
    await loadComments()
  } catch { /* handled */ }
}

function scrollToComment() {
  nextTick(() => {
    document.querySelector('.comment-form__input-area')?.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
  })
}

onUnmounted(() => {
  cleanupTocObserver()
})
</script>

<style lang="scss" scoped>
.post-detail {
  display: flex;
  gap: var(--spacing-xl);
  max-width: calc(860px + 200px + var(--spacing-xl));
  margin: 0 auto;
  padding: 0 var(--spacing-md);
  position: relative;
}

// ── TOC Sidebar ──
.toc-sidebar {
  width: 200px;
  flex-shrink: 0;
  position: sticky;
  top: calc(var(--header-height) + var(--spacing-lg));
  align-self: flex-start;
  max-height: calc(100vh - var(--header-height) - var(--spacing-2xl));
  overflow-y: auto;
  padding-right: var(--spacing-md);
  border-right: 1px solid var(--color-border);

  &::-webkit-scrollbar { width: 3px; }
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--spacing-sm);
  padding-left: 12px;
}

.toc-link {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  line-height: 1.5;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all var(--transition-fast);
  text-decoration: none;
  border-left: 2px solid transparent;
  cursor: pointer;

  &:hover {
    color: var(--color-text-primary);
    background: var(--color-bg-secondary);
  }

  &.is-active {
    color: var(--color-primary);
    background: var(--color-primary-50);
    border-left-color: var(--color-primary);
    font-weight: var(--font-weight-medium);
  }
}

// ── Main ──
.detail-main {
  flex: 1;
  min-width: 0;
}

// ── Card ──
.detail-card {
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-lg);
}

// ── Badges ──
.detail-badges {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);

  &__left {
    display: flex;
    gap: var(--spacing-sm);
  }
}

.view-count-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-medium);
}

.badge-pin {
  color: var(--color-danger);
  background: #FEF2F2;
}

.badge-featured {
  color: var(--color-warning);
  background: #FFFBEB;
}

.badge-expiry {
  font-size: 11px;
  opacity: 0.7;
  margin-left: 2px;
}

// ── Title ──
.detail-title {
  font-size: 34px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
  margin-bottom: var(--spacing-lg);
  letter-spacing: -0.3px;
}

// ── Author Row ──
.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);

  &__left {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.author-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  transition: opacity var(--transition-fast);

  &:hover { opacity: 0.8; }
}

.author-avatar {
  flex-shrink: 0;
  box-shadow: 0 0 0 2px var(--color-bg);
}

.author-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.author-sub {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.edited-mark {
  font-style: italic;
}

// ── Content Body ──
.detail-body {
  padding-bottom: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);
}

// ── Meta Bottom ──
.detail-meta-bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-md);
  padding-bottom: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);
}

.category-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: var(--color-primary-50);
  color: var(--color-primary);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-primary);
    color: #fff;
  }
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.tag-chip {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  padding: 4px 10px;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
    background: var(--color-primary-50);
  }
}

// ── Bottom Bar ──
.bottom-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);

  &__left {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 2px;
  }
}

// ── Action Button ──
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  background: none;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
  }

  &.is-active {
    color: var(--color-primary);
    font-weight: 500;
  }

  &--like {
    font-weight: 500;
    color: var(--color-text-primary);
    font-size: var(--font-size-md);

    &.is-active {
      color: #E05252;
    }
  }

  &--muted {
    opacity: 0.45;
    padding: 4px 10px;
    font-size: var(--font-size-xs);

    &:hover {
      opacity: 0.8;
      background: #FEF2F2;
      color: var(--color-danger);
    }
  }

  &.danger:hover {
    color: var(--color-danger);
    background: #FEF2F2;
  }
}

@media (max-width: 767px) {
  .action-btn {
    padding: 6px 8px;

    &--like {
      font-size: var(--font-size-sm);
    }

    &--muted {
      display: none;
    }
  }
}

// ── Series Navigation ──
.series-nav {
  padding: var(--spacing-lg) var(--spacing-xl);
  margin-bottom: var(--spacing-lg);

  &__header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-md);
    padding-bottom: var(--spacing-sm);
    border-bottom: 1px solid var(--color-divider);
  }

  &__name {
    font-weight: var(--font-weight-medium);
    color: var(--color-primary);
    &:hover { text-decoration: underline; }
  }

  &__posts {
    display: flex;
    justify-content: space-between;
    gap: var(--spacing-md);
  }

  &__link {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    max-width: 48%;
    padding: var(--spacing-sm) var(--spacing-md);
    border-radius: var(--radius-sm);
    background: var(--color-bg-secondary);
    transition: all var(--transition-fast);

    &:hover:not(.disabled) {
      background: var(--color-primary-50);
      color: var(--color-primary);
    }

    &.prev { justify-content: flex-start; }
    &.next { justify-content: flex-end; text-align: right; }
    &.disabled {
      color: var(--color-text-tertiary);
      cursor: default;
    }
  }

  &__label {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    flex-shrink: 0;
  }

  &__post-title {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// ── Related Posts ──
.related-section {
  margin-bottom: var(--spacing-lg);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 0;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-base);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    background: var(--color-bg-secondary);
    color: var(--color-primary);
  }

  &:active {
    transform: rotate(180deg);
    transition: transform 0.3s ease;
  }
}

.comment-count {
  font-weight: var(--font-weight-normal);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-base);
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--spacing-md);
}

.related-card {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-lg);
  transition: all var(--transition-base);

  &__title {
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
    margin-bottom: var(--spacing-sm);
    line-height: var(--line-height-tight);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__summary {
    font-size: var(--font-size-sm);
    color: var(--color-text-tertiary);
    flex: 1;
    margin-bottom: var(--spacing-sm);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }
}

// ── Comment Sort ──
.comment-sort {
  display: flex;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-md);
}

.sort-btn {
  padding: 4px 14px;
  border: none;
  background: none;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-family: var(--font-family);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover { background: var(--color-bg-secondary); }

  &.active {
    background: var(--color-primary-50);
    color: var(--color-primary);
    font-weight: var(--font-weight-medium);
  }
}

// ── Virtual Scroll ──
.comment-list-virtual {
  height: 600px;
  overflow-y: auto;
  contain: strict;
}

// ── Comment Login CTA ──
.comment-login-cta {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--color-primary);
    background: var(--color-primary-50);
  }

  &__avatar {
    flex-shrink: 0;
    background: var(--color-border);
    color: var(--color-text-tertiary);
  }

  &__input {
    flex: 1;
    font-size: var(--font-size-sm);
    color: var(--color-text-tertiary);
  }
}

// ── Comments ──
.comments-section {
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-xl);
}

.comment-form {
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);

  &__input-wrap {
    display: flex;
    gap: var(--spacing-md);
  }

  &__avatar {
    flex-shrink: 0;
    margin-top: 2px;
  }

  &__input-area {
    flex: 1;
    min-width: 0;
  }

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

.reply-indicator {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px var(--spacing-md);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);

  strong { color: var(--color-primary); }
}

.reply-cancel {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: var(--font-size-xs);

  &:hover { color: var(--color-danger); }
}

.comment-more {
  text-align: center;
  padding-top: var(--spacing-lg);
}

// ── @Mention Panel ──
.mention-panel {
  position: fixed;
  z-index: 200;
  width: 280px;
  max-height: 220px;
  overflow-y: auto;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-xs);

  &__loading,
  &__empty {
    padding: var(--spacing-md);
    color: var(--color-text-tertiary);
    font-size: var(--font-size-sm);
    text-align: center;
  }
}

.mention-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-family);
  transition: background var(--transition-fast);

  &:hover {
    background: var(--color-bg-secondary);
  }

  &__info {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 1px;
  }

  &__name {
    font-size: var(--font-size-sm);
    color: var(--color-text-primary);
    font-weight: var(--font-weight-medium);
  }

  &__username {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }
}

// ── Image Preview Overlay ──
.image-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl);
  cursor: zoom-out;

  &__close {
    position: fixed;
    top: var(--spacing-lg);
    right: var(--spacing-lg);
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border: none;
    background: rgba(255, 255, 255, 0.12);
    color: #fff;
    border-radius: var(--radius-full);
    cursor: pointer;
    transition: background var(--transition-fast);
    z-index: 1001;

    &:hover { background: rgba(255, 255, 255, 0.25); }
  }

  &__img {
    max-width: 90vw;
    max-height: 90vh;
    object-fit: contain;
    border-radius: var(--radius-sm);
    box-shadow: 0 16px 64px rgba(0, 0, 0, 0.4);
    cursor: default;
  }
}

// ── Responsive ──
@media (max-width: 1100px) {
  .toc-sidebar {
    display: none;
  }

  .post-detail {
    max-width: 860px;
  }
}

@media (max-width: 767px) {
  .post-detail {
    padding: 0;
  }

  .detail-card {
    padding: var(--spacing-md);
    border-radius: 0;
    border-left: none;
    border-right: none;
  }

  .detail-title {
    font-size: 22px;
  }

  .author-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .comments-section {
    padding: var(--spacing-md);
    border-radius: 0;
    border-left: none;
    border-right: none;
  }

  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<!-- Global (non-scoped) styles for markdown-body rendered HTML -->
<style lang="scss">
.markdown-body {
  font-size: var(--font-size-md);
  line-height: var(--line-height-relaxed);
  color: var(--color-text-primary);
  word-break: break-word;

  h1, h2, h3, h4, h5, h6 {
    margin-top: var(--spacing-xl);
    margin-bottom: var(--spacing-md);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-tight);
    color: var(--color-text-primary);

    &:first-child { margin-top: 0; }
  }

  h1 { font-size: 1.6em; border-bottom: 1px solid var(--color-divider); padding-bottom: var(--spacing-sm); }
  h2 { font-size: 1.35em; border-bottom: 1px solid var(--color-divider); padding-bottom: var(--spacing-xs); }
  h3 { font-size: 1.15em; }
  h4 { font-size: 1.05em; }

  p {
    margin-bottom: var(--spacing-md);
    &:last-child { margin-bottom: 0; }
  }

  strong { font-weight: var(--font-weight-semibold); }
  em { font-style: italic; }
  del { text-decoration: line-through; color: var(--color-text-tertiary); }

  a {
    color: var(--color-primary);
    text-decoration: none;
    border-bottom: 1px solid transparent;
    transition: border-color var(--transition-fast);

    &:hover {
      border-bottom-color: var(--color-primary);
    }
  }

  .inline-code {
    background: var(--color-bg-secondary);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.88em;
    font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', Consolas, monospace;
    color: #E05252;
    word-break: break-all;

    html.dark & {
      color: #F87171;
    }
  }

  // ── Code Blocks ──
  .code-block-wrapper {
    margin: var(--spacing-lg) 0;
    border-radius: var(--radius-md);
    overflow: hidden;
    border: 1px solid var(--color-border);
    background: #1E1E2E;
    box-shadow: var(--shadow-sm);
  }

  .code-block-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 14px;
    background: #181825;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  .code-lang {
    font-size: var(--font-size-xs);
    color: #94A3B8;
    font-weight: var(--font-weight-medium);
    text-transform: lowercase;
    font-family: var(--font-family);
  }

  .code-copy-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;
    border: none;
    background: none;
    color: #64748B;
    cursor: pointer;
    border-radius: 4px;
    transition: all var(--transition-fast);

    &:hover { color: #E2E8F0; background: rgba(255, 255, 255, 0.06); }
    &.copied { color: #34D399; }
  }

  pre {
    margin: 0;
    padding: var(--spacing-lg);
    overflow-x: auto;
    background: #1E1E2E;
    font-size: var(--font-size-sm);
    line-height: 1.65;

    code {
      font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
      color: #E2E8F0;
    }
  }

  // Syntax highlighting tokens
  .token.keyword  { color: #C084FC; }
  .token.string   { color: #34D399; }
  .token.comment  { color: #64748B; font-style: italic; }
  .token.number   { color: #FBBF24; }
  .token.function { color: #60A5FA; }
  .token.plain    { color: #E2E8F0; }

  // ── Other elements ──
  blockquote {
    border-left: 3px solid var(--color-primary);
    padding: var(--spacing-sm) var(--spacing-lg);
    margin: var(--spacing-lg) 0;
    background: var(--color-bg-secondary);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
    color: var(--color-text-secondary);

    p { margin-bottom: var(--spacing-xs); }
  }

  ul, ol {
    padding-left: var(--spacing-xl);
    margin-bottom: var(--spacing-md);

    li { margin-bottom: var(--spacing-xs); }
  }

  ul li { list-style-type: disc; }
  ol li { list-style-type: decimal; }

  hr {
    border: none;
    border-top: 1px solid var(--color-divider);
    margin: var(--spacing-xl) 0;
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin: var(--spacing-lg) 0;
    font-size: var(--font-size-sm);

    th, td {
      padding: var(--spacing-sm) var(--spacing-md);
      border: 1px solid var(--color-border);
      text-align: left;
    }

    th {
      background: var(--color-bg-secondary);
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
    }

    td {
      color: var(--color-text-secondary);
    }
  }

  // ── Content Images ──
  .content-image-figure {
    display: block;
    width: 100%;
    margin: var(--spacing-xl) 0;
    padding: 0;
    text-align: center;

    img.content-image {
      display: block;
      width: 100%;
      max-width: 100%;
      height: auto;
      border-radius: var(--radius-md);
      cursor: zoom-in;
      box-shadow: var(--shadow-sm);
      transition: box-shadow var(--transition-fast);

      &:hover {
        box-shadow: var(--shadow-md);
      }
    }
  }

  // ── Mermaid Diagrams ──
  .mermaid-wrapper {
    margin: var(--spacing-lg) 0;
    padding: var(--spacing-lg);
    background: var(--color-bg-secondary);
    border-radius: var(--radius-md);
    border: 1px solid var(--color-border);
    overflow-x: auto;
    text-align: center;

    .mermaid {
      display: flex;
      justify-content: center;

      svg {
        max-width: 100%;
        height: auto;
      }
    }

    .mermaid-error {
      color: var(--color-text-tertiary);
      font-size: var(--font-size-sm);
      padding: var(--spacing-md);
    }
  }

  // ── Code Sandbox Embeds ──
  .sandbox-embed {
    margin: var(--spacing-lg) 0;
    border-radius: var(--radius-md);
    overflow: hidden;
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-sm);

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 14px;
      background: var(--color-bg-secondary);
      border-bottom: 1px solid var(--color-border);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
      color: var(--color-text-secondary);
    }

    &__link {
      font-size: var(--font-size-xs);
      color: var(--color-primary);
      &:hover { text-decoration: underline; }
    }

    &__iframe {
      position: relative;
      width: 100%;
      height: 0;
      padding-bottom: 56.25%;

      iframe {
        position: absolute;
        inset: 0;
        width: 100%;
        height: 100%;
        border: none;
      }
    }
  }
}
</style>
