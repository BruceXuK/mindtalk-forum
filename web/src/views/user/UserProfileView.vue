<template>
  <div class="profile-page" v-loading="loading">
    <template v-if="user">
      <div class="profile-header card-base">
        <el-avatar :size="88" :src="user.avatarUrl" class="profile-avatar">
          {{ user.nickname?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="profile-info">
          <h2 class="profile-name">{{ user.nickname }}</h2>
          <p class="profile-bio" v-if="user.bio">{{ user.bio }}</p>
          <div class="profile-stats">
            <span class="stat-item"><strong>{{ user.followingCount || 0 }}</strong> 关注</span>
            <span class="stat-item"><strong>{{ user.followerCount || 0 }}</strong> 粉丝</span>
            <span class="stat-item"><strong>{{ user.postCount || 0 }}</strong> 帖子</span>
          </div>
          <div class="profile-meta" v-if="user.location">
            <el-icon :size="14"><Location /></el-icon> {{ user.location }}
          </div>
          <div class="profile-badges" v-if="badges.length > 0">
            <el-tooltip
              v-for="b in badges"
              :key="b.id"
              :content="`${b.name} — ${b.description}`"
              placement="top"
            >
              <div class="badge-chip">
                <el-icon :size="18"><Medal /></el-icon>
                <span>{{ b.name }}</span>
              </div>
            </el-tooltip>
          </div>
        </div>
        <div class="profile-actions">
          <el-button v-if="isLoggedIn && !isSelf" :type="isFollowing ? 'default' : 'primary'" size="small" round @click="toggleFollow">
            {{ isFollowing ? '已关注' : '+ 关注' }}
          </el-button>
          <el-button v-if="isLoggedIn && !isSelf" size="small" round @click="startChat">发私信</el-button>
          <el-button v-if="isSelf" size="small" round @click="showEdit = true">编辑资料</el-button>
        </div>
      </div>

      <!-- 编辑资料弹窗 -->
      <el-dialog v-model="showEdit" title="编辑资料" width="440px">
        <el-form :model="editForm" label-position="top">
          <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
          <el-form-item label="个人简介"><el-input v-model="editForm.bio" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="所在地"><el-input v-model="editForm.location" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button></el-form-item>
        </el-form>
      </el-dialog>

      <!-- Tab: Posts / Series / Drafts / History / ReadLater -->
      <div class="profile-tabs" v-if="isSelf">
        <button class="tab-btn" :class="{ active: activeTab === 'posts' }" @click="activeTab = 'posts'">
          帖子
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'series' }" @click="switchToSeries">
          系列
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'drafts' }" @click="switchToDrafts">
          草稿箱
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'readLater' }" @click="switchToReadLater">
          稍后阅读
        </button>
        <button class="tab-btn" :class="{ active: activeTab === 'history' }" @click="switchToHistory">
          阅读历史
        </button>
      </div>

      <!-- 用户帖子 -->
      <div class="profile-posts" v-if="activeTab === 'posts'">
        <h3 class="section-title" v-if="!isSelf">TA 的帖子</h3>
        <PostCard v-for="p in userPosts" :key="p.id" :post="p" />
        <EmptyState v-if="userPosts.length === 0" title="暂无帖子" description="还没有发布过帖子" />
      </div>

      <!-- 系列列表 -->
      <div class="profile-posts" v-if="activeTab === 'series'">
        <div class="series-header-row">
          <h3 class="section-title">我的系列</h3>
          <el-button size="small" type="primary" @click="$router.push('/series/create')">创建系列</el-button>
        </div>
        <div v-if="seriesList.length > 0" class="series-grid">
          <div
            v-for="s in seriesList"
            :key="s.id"
            class="series-card card-base hover-lift"
            @click="$router.push(`/series/${s.id}`)"
          >
            <div class="series-card__cover" v-if="s.coverUrl">
              <img :src="s.coverUrl" :alt="s.title" />
            </div>
            <div class="series-card__body">
              <h4 class="series-card__title">{{ s.title }}</h4>
              <p class="series-card__desc" v-if="s.description">{{ s.description }}</p>
              <div class="series-card__meta">
                <span>{{ s.postCount }} 篇</span>
                <span>{{ formatDate(s.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <EmptyState v-else title="暂无系列" description="还没有创建过系列" />
      </div>

      <!-- 草稿箱 -->
      <div class="profile-posts" v-if="activeTab === 'drafts'">
        <h3 class="section-title">草稿箱</h3>
        <PostCard v-for="p in drafts" :key="p.id" :post="p" />
        <EmptyState v-if="drafts.length === 0" title="暂无草稿" description="还没有保存的草稿" />
      </div>

      <!-- 稍后阅读 -->
      <div class="profile-posts" v-if="activeTab === 'readLater'">
        <h3 class="section-title">稍后阅读</h3>
        <div v-if="readLaterItems.length > 0" class="history-list">
          <div v-for="item in readLaterItems" :key="item.id" class="history-item">
            <router-link :to="`/posts/${item.postId}`" class="history-item__title">{{ item.postTitle }}</router-link>
            <span class="history-item__time">{{ formatDate(item.createTime) }}</span>
            <button class="history-item__del" @click="removeReadLater(item.postId)">移除</button>
          </div>
        </div>
        <EmptyState v-else title="暂无内容" description="还没有标记稍后阅读的帖子" />
      </div>

      <!-- 阅读历史 -->
      <div class="profile-posts" v-if="activeTab === 'history'">
        <div class="history-header">
          <h3 class="section-title">阅读历史</h3>
          <el-button v-if="historyItems.length > 0" size="small" text type="danger" @click="clearHistory">清空全部</el-button>
        </div>
        <div v-if="historyItems.length > 0" class="history-list">
          <div v-for="item in historyItems" :key="item.id" class="history-item">
            <router-link :to="`/posts/${item.postId}`" class="history-item__title">{{ item.postTitle }}</router-link>
            <span class="history-item__time">{{ formatRelativeTime(item.readAt) }}</span>
            <button class="history-item__del" @click="deleteHistory(item.id)">删除</button>
          </div>
        </div>
        <EmptyState v-else title="暂无阅读历史" description="浏览帖子后将自动记录在这里" />
      </div>
    </template>
    <EmptyState v-else-if="!loading" title="用户不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { userApi } from '@/api/modules/user'
import { postApi } from '@/api/modules/post'
import { seriesApi } from '@/api/modules/series'
import { readingHistoryApi } from '@/api/modules/readingHistory'
import { readLaterApi } from '@/api/modules/readLater'
import { badgeApi } from '@/api/modules/badge'
import { chatApi } from '@/api/modules/chat'
import { useUserStore } from '@/stores/modules/user'
import type { UserProfileVO, PostVO, SeriesVO, ReadingHistoryVO, ReadLaterVO, BadgeVO } from '@/types'
import PostCard from '@/components/post/PostCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { ElMessage } from 'element-plus'
import { Location, Medal } from '@element-plus/icons-vue'
import { formatRelativeTime } from '@/utils'

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.split('T')[0]
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const user = ref<UserProfileVO | null>(null)
const userPosts = ref<PostVO[]>([])
const drafts = ref<PostVO[]>([])
const draftCount = ref(0)
const seriesList = ref<SeriesVO[]>([])
const seriesCount = ref(0)
const historyItems = ref<ReadingHistoryVO[]>([])
const readLaterItems = ref<ReadLaterVO[]>([])
const badges = ref<BadgeVO[]>([])
const activeTab = ref<'posts' | 'series' | 'drafts' | 'readLater' | 'history'>('posts')
const loading = ref(false)
const isFollowing = ref(false)
const showEdit = ref(false)
const saving = ref(false)
const editForm = ref({ nickname: '', bio: '', location: '' })

const userId = computed(() => route.path === '/profile' ? userStore.userInfo?.id : Number(route.params.id))
const isSelf = computed(() => userId.value === userStore.userInfo?.id)
const isLoggedIn = computed(() => userStore.isLoggedIn)

onMounted(loadProfile)

async function loadProfile() {
  if (!userId.value) return
  loading.value = true
  try {
    const res = await userApi.getUserProfile(userId.value)
    user.value = res.data; isFollowing.value = res.data.isFollowing || false
    editForm.value = { nickname: res.data.nickname || '', bio: res.data.bio || '', location: res.data.location || '' }
    loadUserPosts()
    loadBadges()
  } finally { loading.value = false }
}

async function loadBadges() {
  if (!userId.value) return
  try {
    const res = await badgeApi.getUserBadges(userId.value)
    badges.value = res.data
  } catch { /* ignore */ }
}

async function loadUserPosts() {
  if (!userId.value) return
  try {
    const res = await postApi.getList({ page: 1, size: 20, userId: userId.value })
    userPosts.value = res.data.records
  } catch { /* ignore */ }
}

async function loadDrafts() {
  if (!userId.value || !isSelf.value) return
  try {
    const res = await postApi.getMyDrafts({ page: 1, size: 50 })
    drafts.value = res.data.records
    draftCount.value = res.data.total
  } catch { /* ignore */ }
}

async function loadSeries() {
  if (!isSelf.value) return
  try {
    const res = await seriesApi.getMySeries()
    seriesList.value = res.data
    seriesCount.value = res.data.length
  } catch { /* ignore */ }
}

function switchToDrafts() {
  activeTab.value = 'drafts'
  loadDrafts()
}

function switchToSeries() {
  activeTab.value = 'series'
  loadSeries()
}

async function loadHistory() {
  if (!isSelf.value) return
  try {
    const res = await readingHistoryApi.getList({ page: 1, size: 50 })
    historyItems.value = res.data.records
  } catch { /* ignore */ }
}

async function loadReadLater() {
  if (!isSelf.value) return
  try {
    const res = await readLaterApi.getList({ page: 1, size: 50 })
    readLaterItems.value = res.data.records
  } catch { /* ignore */ }
}

function switchToHistory() {
  activeTab.value = 'history'
  loadHistory()
}

function switchToReadLater() {
  activeTab.value = 'readLater'
  loadReadLater()
}

async function deleteHistory(id: number) {
  try {
    await readingHistoryApi.delete(id)
    historyItems.value = historyItems.value.filter(h => h.id !== id)
  } catch { /* ignore */ }
}

async function clearHistory() {
  try {
    await readingHistoryApi.clearAll()
    historyItems.value = []
    ElMessage.success('已清空阅读历史')
  } catch { /* ignore */ }
}

async function removeReadLater(postId: number) {
  try {
    await readLaterApi.remove(postId)
    readLaterItems.value = readLaterItems.value.filter(r => r.postId !== postId)
    ElMessage.success('已移除')
  } catch { /* ignore */ }
}

async function startChat() {
  if (!user.value) return
  try {
    const res = await chatApi.startConversation(user.value.id)
    router.push('/messages/chat')
  } catch { /* ignore */ }
}

async function toggleFollow() {
  if (!user.value) return
  try {
    if (isFollowing.value) { await userApi.unfollowUser(user.value.id); isFollowing.value = false }
    else { await userApi.followUser(user.value.id); isFollowing.value = true }
    loadProfile()
  } catch { /* handled */ }
}

async function saveProfile() {
  saving.value = true
  try { await userApi.updateProfile(editForm.value); ElMessage.success('保存成功'); showEdit.value = false; loadProfile() }
  catch { /* handled */ }
  finally { saving.value = false }
}
</script>

<style lang="scss" scoped>
.profile-page { width: 100%; }
.profile-header { display: flex; align-items: flex-start; gap: var(--spacing-xl); padding: var(--spacing-xl); margin-bottom: var(--spacing-lg); }
.profile-avatar { flex-shrink: 0; }
.profile-info { flex: 1; }
.profile-name { font-size: var(--font-size-xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); margin-bottom: var(--spacing-xs); }
.profile-bio { font-size: var(--font-size-base); color: var(--color-text-secondary); margin-bottom: var(--spacing-md); line-height: var(--line-height-normal); }
.profile-stats { display: flex; gap: var(--spacing-lg); margin-bottom: var(--spacing-xs); }
.stat-item { font-size: var(--font-size-sm); color: var(--color-text-secondary); strong { color: var(--color-text-primary); } }
.profile-meta { font-size: var(--font-size-sm); color: var(--color-text-tertiary); display: flex; align-items: center; gap: 4px; }
.profile-badges { display: flex; flex-wrap: wrap; gap: var(--spacing-sm); margin-top: var(--spacing-md); }
.badge-chip { display: inline-flex; align-items: center; gap: 4px; padding: 2px 10px; border-radius: var(--radius-full); background: linear-gradient(135deg, #FEF3C7, #FDE68A); color: #92400E; font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); }
.profile-actions { flex-shrink: 0; }
.profile-tabs { display: flex; gap: var(--spacing-sm); margin-bottom: var(--spacing-lg); padding-bottom: var(--spacing-md); border-bottom: 1px solid var(--color-divider); }
.tab-btn { display: flex; align-items: center; gap: 6px; padding: 6px 16px; border: none; background: none; border-radius: var(--radius-full); font-size: var(--font-size-sm); font-family: var(--font-family); color: var(--color-text-secondary); cursor: pointer; transition: all var(--transition-fast); &.active { background: var(--color-primary-50); color: var(--color-primary); font-weight: var(--font-weight-medium); } &:hover:not(.active) { background: var(--color-bg-secondary); } }
.draft-count { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 5px; border-radius: var(--radius-full); background: var(--color-primary); color: #fff; font-size: 11px; font-weight: var(--font-weight-medium); }
.section-title { font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: var(--spacing-md); }
.series-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-md); .section-title { margin-bottom: 0; } }
.series-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--spacing-md); }
.series-card { padding: var(--spacing-lg); cursor: pointer; transition: all var(--transition-base);
  &__cover { width: 100%; height: 140px; border-radius: var(--radius-sm); overflow: hidden; margin-bottom: var(--spacing-md); img { width: 100%; height: 100%; object-fit: cover; } }
  &__body { display: flex; flex-direction: column; gap: var(--spacing-xs); }
  &__title { font-size: var(--font-size-base); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
  &__desc { font-size: var(--font-size-sm); color: var(--color-text-tertiary); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  &__meta { display: flex; gap: var(--spacing-md); font-size: var(--font-size-xs); color: var(--color-text-tertiary); margin-top: var(--spacing-xs); }
}
.history-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-md); .section-title { margin-bottom: 0; } }
.history-list { display: flex; flex-direction: column; }
.history-item { display: flex; align-items: center; gap: var(--spacing-md); padding: var(--spacing-sm) 0; border-bottom: 1px solid var(--color-divider);
  &__title { flex: 1; font-size: var(--font-size-sm); color: var(--color-text-primary); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; &:hover { color: var(--color-primary); } }
  &__time { font-size: var(--font-size-xs); color: var(--color-text-tertiary); white-space: nowrap; flex-shrink: 0; }
  &__del { padding: 2px 8px; border: none; background: none; font-size: var(--font-size-xs); color: var(--color-text-tertiary); cursor: pointer; border-radius: 4px; font-family: var(--font-family); flex-shrink: 0; &:hover { color: var(--color-danger); background: #FEF2F2; } }
}
@media (max-width: 767px) { .profile-header { flex-direction: column; align-items: center; text-align: center; } .profile-stats { justify-content: center; } .series-grid { grid-template-columns: 1fr; } }
</style>
