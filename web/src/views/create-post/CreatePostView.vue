<template>
  <div class="create-post-page">
    <!-- Top Bar -->
    <div class="top-bar">
      <button class="top-bar__close" @click="handleClose">
        <el-icon><ArrowLeft /></el-icon>
      </button>
      <div class="top-bar__info">
        <span v-if="draftStatus" class="top-bar__draft-status">
          <el-icon><Clock /></el-icon>
          {{ draftStatus }}
        </span>
      </div>
      <div class="top-bar__actions">
        <button
          class="top-bar__mode-btn"
          :class="{ 'is-active': isPreview }"
          @click="isPreview = !isPreview"
        >
          <el-icon><View /></el-icon>
          <span>{{ isPreview ? '编辑' : '预览' }}</span>
        </button>
        <button
          v-if="!isEdit"
          class="top-bar__draft-btn"
          :disabled="!canSaveDraft || submitting"
          @click="handleSaveDraft"
        >
          <el-icon v-if="draftSaving" class="spinner"><Loading /></el-icon>
          <span>{{ draftSaving ? '保存中' : '存草稿' }}</span>
        </button>
        <button
          class="top-bar__publish-btn"
          :disabled="(!canPublish && !isEdit) || submitting"
          @click="handleSubmit"
        >
          <el-icon v-if="submitting" class="spinner"><Loading /></el-icon>
          <span>{{ isEdit ? (isDraft ? '发布草稿' : '保存修改') : '发布' }}</span>
        </button>
      </div>
    </div>

    <!-- Main Content -->
    <div class="editor-body">
      <!-- Title -->
      <div class="title-area">
        <textarea
          ref="titleInputRef"
          v-model="form.title"
          class="title-input"
          :placeholder="titlePlaceholder"
          rows="1"
          maxlength="200"
          @input="onTitleInput"
          @focus="titleFocused = true"
          @blur="titleFocused = false"
        ></textarea>
      </div>

      <!-- Meta Row: Category + Tags -->
      <div class="meta-row">
        <div class="meta-row__item">
          <label class="meta-row__label">
            <el-icon><Folder /></el-icon>
            分类
          </label>
          <div v-if="categoryLoading" class="meta-row__skeleton">
            <span class="skeleton-chip" v-for="i in 3" :key="i"></span>
          </div>
          <div v-else class="chip-group">
            <button
              v-for="cat in categories"
              :key="cat.id"
              class="chip"
              :class="{ 'is-active': form.categoryId === cat.id }"
              @click="form.categoryId = cat.id"
            >
              {{ cat.icon }} {{ cat.name }}
            </button>
          </div>
        </div>

        <div class="meta-row__item">
          <label class="meta-row__label">
            <el-icon><PriceTag /></el-icon>
            标签
          </label>
          <div v-if="tagLoading" class="meta-row__skeleton">
            <span class="skeleton-chip" v-for="i in 5" :key="i"></span>
          </div>
          <div v-else class="chip-group">
            <button
              v-for="tag in tags"
              :key="tag.id"
              class="chip chip--tag"
              :class="{ 'is-active': form.tagIds.includes(tag.id) }"
              @click="toggleTag(tag.id)"
            >
              {{ tag.name }}
            </button>
          </div>
        </div>

        <div class="meta-row__item" v-if="!isEdit">
          <label class="meta-row__label">
            <el-icon><Collection /></el-icon>
            系列
          </label>
          <div v-if="seriesLoading" class="meta-row__skeleton">
            <span class="skeleton-chip" v-for="i in 2" :key="i"></span>
          </div>
          <div v-else-if="userSeries.length > 0" class="chip-group">
            <button
              class="chip"
              :class="{ 'is-active': form.seriesId === null }"
              @click="form.seriesId = null"
            >
              无
            </button>
            <button
              v-for="s in userSeries"
              :key="s.id"
              class="chip"
              :class="{ 'is-active': form.seriesId === s.id }"
              @click="form.seriesId = s.id"
            >
              {{ s.title }}
            </button>
          </div>
          <span v-else class="meta-row__hint">暂无系列</span>
        </div>
      </div>

      <!-- Editor / Preview -->
      <div class="editor-area">
        <div v-show="!isPreview" class="editor-pane">
          <div class="editor-toolbar">
            <div class="toolbar-group">
              <button
                v-for="btn in toolbarButtons"
                :key="btn.title"
                class="toolbar-btn"
                :title="btn.title"
                @click="btn.action"
                v-html="btn.label"
              ></button>
            </div>
            <span class="toolbar-count">{{ form.contentText.length }} / 50000</span>
          </div>
          <textarea
            ref="contentTextareaRef"
            v-model="form.contentText"
            class="content-input"
            placeholder="开始写作... 输入 @ 可以提及用户"
            maxlength="50000"
            @input="onEditorInput"
            @keydown="onEditorKeydown"
          ></textarea>

          <!-- @提及用户建议面板 -->
          <Transition name="fade">
            <div v-if="mentionVisible" class="mention-panel" :style="{ top: mentionPos.top + 'px', left: mentionPos.left + 'px' }">
              <div v-if="mentionLoading" class="mention-panel__loading">搜索中...</div>
              <div v-else-if="mentionUsers.length === 0" class="mention-panel__empty">未找到用户</div>
              <button
                v-for="user in mentionUsers"
                :key="user.id"
                class="mention-item"
                @click="onSelectMention(user)"
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

        <div v-show="isPreview" class="preview-pane">
          <div v-if="!form.contentText" class="preview-empty">
            <el-icon><Document /></el-icon>
            <p>暂无内容</p>
          </div>
          <div v-else class="markdown-body" v-html="renderedMarkdown"></div>
        </div>
      </div>

      <!-- Upload Zone -->
      <div
        class="upload-zone"
        :class="{ 'is-dragover': isDragOver }"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop"
      >
        <div class="upload-zone__inner">
          <el-icon class="upload-zone__icon"><UploadFilled /></el-icon>
          <p class="upload-zone__text">拖拽图片到此处上传</p>
          <p class="upload-zone__hint">支持 JPG、PNG、GIF、WebP，单张不超过 5MB</p>
          <button class="upload-zone__btn" @click="triggerFileInput">
            <el-icon><Plus /></el-icon>
            选择图片
          </button>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            multiple
            class="upload-zone__file-input"
            @change="handleFileSelect"
          />
        </div>
      </div>

      <!-- Uploaded Images -->
      <div v-if="uploadedImages.length > 0" class="uploaded-list">
        <div v-for="(img, idx) in uploadedImages" :key="idx" class="uploaded-item" :class="{ 'is-uploading': img.uploading, 'is-error': img.error }">
          <img v-if="!img.error" :src="img.url" :alt="img.name" class="uploaded-item__img" />
          <div v-if="img.uploading" class="uploaded-item__overlay">
            <el-icon class="spinner"><Loading /></el-icon>
          </div>
          <div v-if="img.error" class="uploaded-item__error">
            <el-icon><WarningFilled /></el-icon>
            <span>上传失败</span>
          </div>
          <button class="uploaded-item__remove" @click="removeImage(idx)">
            <el-icon><Close /></el-icon>
          </button>
          <span class="uploaded-item__markdown">{{ img.uploading ? '上传中...' : `![${img.name}](${img.url})` }}</span>
          <button class="uploaded-item__copy" @click="copyImageMarkdown(img)" :disabled="img.uploading">
            <el-icon><CopyDocument /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- Fixed Bottom Bar (mobile) -->
    <div class="bottom-bar">
      <div class="bottom-bar__inner">
        <button
          class="bottom-bar__mode-btn"
          :class="{ 'is-active': isPreview }"
          @click="isPreview = !isPreview"
        >
          <el-icon><View /></el-icon>
          {{ isPreview ? '编辑' : '预览' }}
        </button>
        <button
          v-if="!isEdit"
          class="bottom-bar__draft-btn"
          :disabled="!canSaveDraft || submitting"
          @click="handleSaveDraft"
        >
          存草稿
        </button>
        <button
          class="bottom-bar__publish-btn"
          :disabled="(!canPublish && !isEdit) || submitting"
          @click="handleSubmit"
        >
          <el-icon v-if="submitting" class="spinner"><Loading /></el-icon>
          {{ isEdit ? (isDraft ? '发布草稿' : '保存修改') : '发布帖子' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Clock, View, Loading, Folder, PriceTag, Collection,
  Document, UploadFilled, Plus, Close, CopyDocument, WarningFilled
} from '@element-plus/icons-vue'
import { postApi } from '@/api/modules/post'
import { seriesApi } from '@/api/modules/series'
import { fileApi } from '@/api/modules/file'
import { renderMarkdown, renderMermaidDiagrams } from '@/composables/useMarkdown'
import { useMention, parseMentionedUsernames } from '@/composables/useMention'
import { useMarkdownToolbar } from '@/composables/useMarkdownToolbar'
import type { ToolbarActions } from '@/composables/useMarkdownToolbar'
import type { CategoryVO, TagVO, UserVO, SeriesVO } from '@/types'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const isDraft = ref(false)
const titlePlaceholder = computed(() => isEdit.value ? '编辑标题...' : '输入一个引人注目的标题...')

// Watch route to update isDraft when navigating to edit page
watch(() => route.params.id, async (newId) => {
  if (newId) {
    try {
      const res = await postApi.getDetail(Number(newId))
      isDraft.value = res.data.status === 0
    } catch { /* ignore */ }
  } else {
    isDraft.value = false
  }
})

// ── Form state ──
const titleInputRef = ref<HTMLTextAreaElement>()
const contentTextareaRef = ref<HTMLTextAreaElement>()
const toolbarActions: ToolbarActions = useMarkdownToolbar(contentTextareaRef, (v) => { form.contentText = v })
const toolbarButtons = [
  { label: '<b>B</b>',   title: '加粗',         action: toolbarActions.bold },
  { label: '<i>I</i>',   title: '斜体',          action: toolbarActions.italic },
  { label: '<s>S</s>',   title: '删除线',        action: toolbarActions.strikethrough },
  { label: '&lt;/&gt;',  title: '行内代码',      action: toolbarActions.inlineCode },
  { label: 'H',          title: '二级标题',      action: toolbarActions.heading },
  { label: '"',          title: '引用',          action: toolbarActions.blockquote },
  { label: '{ }',        title: '代码块',        action: toolbarActions.codeBlock },
  { label: '•',          title: '无序列表',      action: toolbarActions.unorderedList },
  { label: '1.',         title: '有序列表',      action: toolbarActions.orderedList },
  { label: '—',          title: '分割线',        action: toolbarActions.divider },
  { label: '🔗',         title: '链接',          action: toolbarActions.link },
]
const fileInputRef = ref<HTMLInputElement>()
const titleFocused = ref(false)

// ── @Mention ──
const {
  mentionVisible, mentionUsers, mentionLoading, mentionPos,
  onInput: onMentionInput, selectUser: applyMention, closeMention
} = useMention()
const mentionedUserIds = ref<number[]>([])
const mentionedUsernames = ref<string[]>([])

function onEditorInput(e: Event) {
  onMentionInput(e)
  // Re-parse mentions in content
  mentionedUsernames.value = parseMentionedUsernames(form.contentText)
}

function onEditorKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeMention()
  }
}

function onSelectMention(user: UserVO) {
  if (!contentTextareaRef.value) return
  const id = applyMention(user, contentTextareaRef.value)
  // Sync form content text after DOM modification
  form.contentText = contentTextareaRef.value.value
  if (!mentionedUserIds.value.includes(id)) {
    mentionedUserIds.value.push(id)
  }
  mentionedUsernames.value = parseMentionedUsernames(form.contentText)
}
const isPreview = ref(false)
const submitting = ref(false)
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const userSeries = ref<SeriesVO[]>([])
const categoryLoading = ref(true)
const tagLoading = ref(true)
const seriesLoading = ref(true)

const form = reactive({
  title: '',
  contentText: '',
  categoryId: null as number | null,
  tagIds: [] as number[],
  seriesId: null as number | null
})

const canPublish = computed(() =>
  form.title.trim().length > 0 && form.categoryId !== null && form.contentText.trim().length > 0
)
const canSaveDraft = computed(() =>
  form.title.trim().length > 0 || form.contentText.trim().length > 0
)

// ── Draft auto-save ──
const DRAFT_KEY = 'mindtalk_draft'
const draftStatus = ref('')
let draftTimer: ReturnType<typeof setTimeout> | null = null
let draftLoaded = false

function saveDraft() {
  const draft = {
    title: form.title,
    contentText: form.contentText,
    categoryId: form.categoryId,
    tagIds: form.tagIds,
    seriesId: form.seriesId,
    timestamp: Date.now()
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
  draftStatus.value = '草稿已保存'
  if (draftTimer) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => { draftStatus.value = '' }, 2000)
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    // 7 天后草稿过期
    if (draft.timestamp && Date.now() - draft.timestamp > 7 * 24 * 3600 * 1000) {
      clearDraft()
      return
    }
    if (draft.title) form.title = draft.title
    if (draft.contentText) form.contentText = draft.contentText
    if (draft.categoryId) form.categoryId = draft.categoryId
    if (draft.tagIds) form.tagIds = draft.tagIds
    if (draft.seriesId !== undefined) form.seriesId = draft.seriesId
    draftLoaded = true
  } catch { /* ignore */ }
}

function clearDraft() {
  localStorage.removeItem(DRAFT_KEY)
}

// Watch for changes to auto-save
watch(
  () => [form.title, form.contentText, form.categoryId, form.tagIds, form.seriesId],
  () => {
    if (!draftLoaded) return
    if (draftTimer) clearTimeout(draftTimer)
    draftTimer = setTimeout(saveDraft, 1500)
  },
  { deep: true }
)

// ── Title auto-resize ──
function onTitleInput() {
  const el = titleInputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

// ── Tags ──
function toggleTag(tagId: number) {
  const idx = form.tagIds.indexOf(tagId)
  if (idx === -1) {
    form.tagIds.push(tagId)
  } else {
    form.tagIds.splice(idx, 1)
  }
}

// ── Image upload ──
const isDragOver = ref(false)
interface UploadedImage {
  name: string
  url: string        // local preview URL or remote URL after upload
  uploading: boolean
  error: boolean
}
const uploadedImages = ref<UploadedImage[]>([])

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files) return
  processFiles(input.files)
  input.value = ''
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  if (!e.dataTransfer?.files) return
  processFiles(e.dataTransfer.files)
}

function processFiles(files: FileList) {
  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (!file.type.startsWith('image/')) {
      ElMessage.warning(`${file.name} 不是图片文件`)
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过 5MB 限制`)
      continue
    }
    const localUrl = URL.createObjectURL(file)
    const idx = uploadedImages.value.length
    uploadedImages.value.push({ name: file.name, url: localUrl, uploading: true, error: false })
    // Insert placeholder markdown
    const placeholder = `\n![${file.name}](上传中...)\n`
    form.contentText += placeholder

    // Upload to server
    fileApi.upload(file).then((res) => {
      const remoteUrl = res.data.url
      // Replace local URL with remote URL
      URL.revokeObjectURL(localUrl)
      uploadedImages.value[idx].url = remoteUrl
      uploadedImages.value[idx].uploading = false
      // Replace placeholder in markdown
      form.contentText = form.contentText.replace(
        `![${file.name}](上传中...)`,
        `![${file.name}](${remoteUrl})`
      )
    }).catch(() => {
      uploadedImages.value[idx].uploading = false
      uploadedImages.value[idx].error = true
      ElMessage.error(`${file.name} 上传失败`)
    })
  }
}

function removeImage(idx: number) {
  const img = uploadedImages.value[idx]
  if (img.uploading) {
    ElMessage.info('图片正在上传中')
    return
  }
  if (!img.error && img.url && !img.url.startsWith('blob:')) {
    URL.revokeObjectURL(img.url)
  }
  uploadedImages.value.splice(idx, 1)
}

function copyImageMarkdown(img: UploadedImage) {
  if (img.uploading) {
    ElMessage.info('图片正在上传中')
    return
  }
  const md = `![${img.name}](${img.url})`
  navigator.clipboard.writeText(md).then(() => {
    ElMessage.success('已复制 Markdown 代码')
  }).catch(() => {
    ElMessage.info(md)
  })
}

// ── Markdown rendering ──
const renderedMarkdown = computed(() => renderMarkdown(form.contentText))

// Watch preview to render mermaid
watch(isPreview, async (val) => {
  if (val) {
    await nextTick()
    const previewEl = document.querySelector('.preview-pane .markdown-body')
    if (previewEl) renderMermaidDiagrams(previewEl as HTMLElement)
  }
})

// ── Navigation ──
function handleClose() {
  if (form.title || form.contentText) {
    ElMessageBox.confirm('有未发布的内容，确定要离开吗？草稿已自动保存。', '提示', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
      type: 'warning'
    }).then(() => {
      saveDraft()
      router.back()
    }).catch(() => { /* stay */ })
  } else {
    router.back()
  }
}

onBeforeRouteLeave((_to, _from, next) => {
  if (form.title || form.contentText) {
    saveDraft()
  }
  next()
})

// ── Submit ──
const draftSaving = ref(false)

async function handleSaveDraft() {
  if (!canSaveDraft.value || submitting.value || draftSaving.value) return
  draftSaving.value = true
  try {
    const data: any = {
      title: form.title.trim() || '无标题草稿',
      content: form.contentText,
      contentText: form.contentText,
      categoryId: form.categoryId || 1,
      tagIds: form.tagIds,
      status: 0,
      mentionedUserIds: mentionedUserIds.value,
      seriesId: form.seriesId,
    }
    if (isEdit.value) {
      await postApi.update(Number(route.params.id), data)
    } else {
      const res = await postApi.create(data)
      router.replace(`/posts/${res.data.id}/edit`)
    }
    ElMessage.success('草稿已保存')
    clearDraft()
  } catch {
    /* handled */
  } finally {
    draftSaving.value = false
  }
}

async function handleSubmit() {
  if (!canPublish.value || submitting.value) return
  submitting.value = true
  try {
    const data: any = {
      title: form.title.trim(),
      content: form.contentText,
      contentText: form.contentText,
      categoryId: form.categoryId!,
      tagIds: form.tagIds,
      mentionedUserIds: mentionedUserIds.value,
      seriesId: form.seriesId,
    }
    if (isEdit.value) {
      await postApi.update(Number(route.params.id), { ...data, status: 1 })
      // If it was a draft, also publish
      if (isDraft.value) {
        await postApi.publishDraft(Number(route.params.id))
        ElMessage.success('草稿已发布')
      } else {
        ElMessage.success('修改成功')
      }
      clearDraft()
      router.push(`/posts/${route.params.id}`)
    } else {
      const res = await postApi.create({ ...data, status: 1 })
      ElMessage.success('发布成功')
      clearDraft()
      router.push(`/posts/${res.data.id}`)
    }
  } catch {
    /* handled by interceptor */
  } finally {
    submitting.value = false
  }
}

// ── Lifecycle ──
onMounted(async () => {
  // Read seriesId from route query
  const querySeriesId = route.query.seriesId
  if (querySeriesId) {
    form.seriesId = Number(querySeriesId)
  }

  // Load draft for new post
  if (!isEdit.value) {
    loadDraft()
    draftLoaded = true
    await nextTick()
    if (form.title) onTitleInput()
  }

  // Load categories and tags
  try {
    const [catRes, tagRes] = await Promise.all([
      postApi.getCategories(),
      postApi.getTags()
    ])
    categories.value = catRes.data
    tags.value = tagRes.data
  } catch {
    /* handled */
  } finally {
    categoryLoading.value = false
    tagLoading.value = false
  }

  // Load user series (for new posts only)
  if (!isEdit.value) {
    try {
      const sRes = await seriesApi.getMySeries()
      userSeries.value = sRes.data
    } catch {
      /* handled */
    } finally {
      seriesLoading.value = false
    }
  } else {
    seriesLoading.value = false
  }

  // Load existing post for edit
  if (isEdit.value) {
    try {
      const id = Number(route.params.id)
      const res = await postApi.getDetail(id)
      form.title = res.data.title
      form.contentText = res.data.contentText || ''
      form.categoryId = res.data.category?.id || null
      form.tagIds = res.data.tags.map(t => t.id)
      isDraft.value = res.data.status === 0
      draftLoaded = true
      await nextTick()
      onTitleInput()
    } catch {
      ElMessage.error('加载帖子失败')
      router.back()
    }
  }
})

onUnmounted(() => {
  if (draftTimer) clearTimeout(draftTimer)
  uploadedImages.value.forEach(img => URL.revokeObjectURL(img.url))
})
</script>

<style lang="scss" scoped>
.create-post-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - var(--header-height));
  max-width: 820px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

// ── Top Bar ──
.top-bar {
  position: sticky;
  top: var(--header-height);
  z-index: 50;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
  background: var(--color-bg);
  border-bottom: 1px solid transparent;
  transition: border-color var(--transition-fast);
  backdrop-filter: blur(8px);

  &__close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: none;
    background: var(--color-card);
    border-radius: var(--radius-full);
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all var(--transition-fast);
    font-size: 18px;
    box-shadow: var(--shadow-sm);
    flex-shrink: 0;

    &:hover {
      color: var(--color-text-primary);
      box-shadow: var(--shadow-md);
      transform: translateX(-1px);
    }
  }

  &__info {
    flex: 1;
  }

  &__draft-status {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--spacing-sm);
    flex-shrink: 0;
  }

  &__mode-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border: 1px solid var(--color-border);
    background: var(--color-card);
    border-radius: var(--radius-full);
    color: var(--color-text-secondary);
    font-size: var(--font-size-sm);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover {
      border-color: var(--color-text-tertiary);
      color: var(--color-text-primary);
    }

    &.is-active {
      background: var(--color-text-primary);
      color: #fff;
      border-color: var(--color-text-primary);
    }
  }

  &__draft-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    border: 1px solid var(--color-border);
    background: var(--color-card);
    color: var(--color-text-secondary);
    border-radius: var(--radius-full);
    font-size: var(--font-size-sm);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover:not(:disabled) {
      border-color: var(--color-text-tertiary);
      color: var(--color-text-primary);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__publish-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 20px;
    border: none;
    background: var(--color-primary);
    color: #fff;
    border-radius: var(--radius-full);
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-semibold);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover:not(:disabled) {
      background: var(--color-primary-hover);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.spinner {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// ── Editor Body ──
.editor-body {
  flex: 1;
  padding: var(--spacing-lg) 0 var(--spacing-2xl);
}

// ── Title Area ──
.title-area {
  margin-bottom: var(--spacing-lg);
}

.title-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 36px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.3;
  resize: none;
  outline: none;
  font-family: var(--font-family);
  overflow: hidden;

  &::placeholder {
    color: var(--color-text-tertiary);
    font-weight: var(--font-weight-normal);
  }
}

// ── Meta Row ──
.meta-row {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding-bottom: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);

  &__item {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  &__label {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    white-space: nowrap;
    padding-top: 6px;
    min-width: 56px;
    flex-shrink: 0;
  }

  &__skeleton {
    display: flex;
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }

  &__hint {
    font-size: var(--font-size-sm);
    color: var(--color-text-tertiary);
    padding-top: 6px;
  }
}

.skeleton-chip {
  display: inline-block;
  width: 64px;
  height: 28px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-border) 50%, var(--color-bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 14px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-family);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &.is-active {
    background: var(--color-primary-50);
    border-color: var(--color-primary);
    color: var(--color-primary);
    font-weight: var(--font-weight-medium);
  }
}

// ── Editor Area ──
.editor-area {
  min-height: 400px;
}

.editor-pane {
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-divider);
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: none;
  border-radius: 6px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-family);

  &:hover {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
  }

  &:active {
    background: var(--color-border);
  }

  :deep(b), :deep(i), :deep(s) {
    font-size: var(--font-size-sm);
  }

  :deep(i) {
    font-family: Georgia, 'Times New Roman', serif;
  }
}

.toolbar-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.content-input {
  width: 100%;
  min-height: 420px;
  border: none;
  background: transparent;
  font-size: var(--font-size-md);
  color: var(--color-text-primary);
  line-height: var(--line-height-relaxed);
  resize: vertical;
  outline: none;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  tab-size: 2;

  &::placeholder {
    color: var(--color-text-tertiary);
    font-family: var(--font-family);
  }
}

// ── Preview Pane ──
.preview-pane {
  min-height: 420px;
}

.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--color-text-tertiary);
  gap: var(--spacing-md);

  .el-icon {
    font-size: 48px;
    opacity: 0.4;
  }

  p {
    font-size: var(--font-size-md);
  }
}

.markdown-body {
  font-size: var(--font-size-md);
  line-height: var(--line-height-relaxed);
  color: var(--color-text-primary);

  :deep(h1) {
    font-size: var(--font-size-2xl);
    font-weight: var(--font-weight-bold);
    margin: var(--spacing-lg) 0 var(--spacing-md);
    line-height: var(--line-height-tight);
    &:first-child { margin-top: 0; }
  }

  :deep(h2) {
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-semibold);
    margin: var(--spacing-lg) 0 var(--spacing-sm);
    line-height: var(--line-height-tight);
  }

  :deep(h3) {
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    margin: var(--spacing-md) 0 var(--spacing-sm);
    line-height: var(--line-height-tight);
  }

  :deep(h4) {
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-semibold);
    margin: var(--spacing-md) 0 var(--spacing-xs);
  }

  :deep(p) {
    margin-bottom: var(--spacing-md);
  }

  :deep(strong) {
    font-weight: var(--font-weight-semibold);
  }

  :deep(em) {
    font-style: italic;
  }

  :deep(del) {
    text-decoration: line-through;
    color: var(--color-text-tertiary);
  }

  :deep(a) {
    color: var(--color-primary);
    text-decoration: underline;
    text-underline-offset: 2px;
    &:hover { color: var(--color-primary-hover); }
  }

  :deep(code) {
    background: var(--color-bg-secondary);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
    font-family: 'SF Mono', 'Fira Code', Consolas, monospace;
    color: var(--color-danger);
  }

  :deep(pre) {
    background: var(--color-bg-secondary);
    padding: var(--spacing-md);
    border-radius: var(--radius-sm);
    overflow-x: auto;
    margin-bottom: var(--spacing-md);

    code {
      background: none;
      padding: 0;
      color: var(--color-text-primary);
      font-size: var(--font-size-sm);
      line-height: var(--line-height-normal);
    }
  }

  :deep(blockquote) {
    border-left: 3px solid var(--color-primary);
    padding-left: var(--spacing-md);
    margin-bottom: var(--spacing-md);
    color: var(--color-text-secondary);

    p { margin-bottom: var(--spacing-xs); }
  }

  :deep(ul), :deep(ol) {
    padding-left: var(--spacing-lg);
    margin-bottom: var(--spacing-md);
  }

  :deep(li) {
    margin-bottom: var(--spacing-xs);
  }

  :deep(hr) {
    border: none;
    border-top: 1px solid var(--color-border);
    margin: var(--spacing-lg) 0;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: var(--radius-sm);
    margin: var(--spacing-md) 0;
  }

  :deep(.sandbox-embed) {
    margin: var(--spacing-lg) 0;
    border-radius: var(--radius-md);
    overflow: hidden;
    border: 1px solid var(--color-border);

    .sandbox-embed__header {
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

    .sandbox-embed__link {
      font-size: var(--font-size-xs);
      color: var(--color-primary);
      &:hover { text-decoration: underline; }
    }

    .sandbox-embed__iframe {
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

// ── Upload Zone ──
.upload-zone {
  margin-top: var(--spacing-xl);
  padding: var(--spacing-xl);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center;
  transition: all var(--transition-fast);

  &:hover,
  &.is-dragover {
    border-color: var(--color-primary);
    background: var(--color-primary-50);
  }

  &__inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--spacing-sm);
  }

  &__icon {
    font-size: 36px;
    color: var(--color-text-tertiary);

    .is-dragover & {
      color: var(--color-primary);
    }
  }

  &__text {
    font-size: var(--font-size-md);
    color: var(--color-text-secondary);
    font-weight: var(--font-weight-medium);
  }

  &__hint {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }

  &__btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    margin-top: var(--spacing-sm);
    padding: 8px 18px;
    border: 1px solid var(--color-border);
    background: var(--color-card);
    border-radius: var(--radius-full);
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all var(--transition-fast);
    font-family: var(--font-family);

    &:hover {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }
  }

  &__file-input {
    display: none;
  }
}

// ── Uploaded Images ──
.uploaded-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
}

.uploaded-item {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  transition: border-color var(--transition-fast);

  &.is-uploading {
    border-color: var(--color-primary);
  }

  &.is-error {
    border-color: var(--color-danger);
  }

  &__img {
    width: 100%;
    height: 140px;
    object-fit: cover;
    display: block;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.3);
    color: #fff;
    font-size: 28px;
  }

  &__error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 140px;
    color: var(--color-danger);
    font-size: var(--font-size-xs);
    gap: var(--spacing-xs);

    .el-icon {
      font-size: 28px;
    }
  }

  &__remove {
    position: absolute;
    top: 6px;
    right: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border: none;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    border-radius: var(--radius-full);
    cursor: pointer;
    font-size: 12px;
    opacity: 0;
    transition: opacity var(--transition-fast);

    .uploaded-item:hover & { opacity: 1; }
  }

  &__markdown {
    display: block;
    padding: 6px 8px;
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-family: monospace;
  }

  &__copy {
    position: absolute;
    bottom: 6px;
    right: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border: none;
    background: var(--color-card);
    color: var(--color-text-secondary);
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    opacity: 0;
    transition: opacity var(--transition-fast);

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }

    .uploaded-item:hover & { opacity: 1; }
    &:hover:not(:disabled) { color: var(--color-primary); }
  }
}

// ── Bottom Bar (mobile) ──
.bottom-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
  padding: var(--spacing-sm) var(--spacing-md);
  padding-bottom: calc(var(--spacing-sm) + env(safe-area-inset-bottom, 0px));

  &__inner {
    display: flex;
    gap: var(--spacing-sm);
  }

  &__mode-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 10px 16px;
    border: 1px solid var(--color-border);
    background: var(--color-card);
    border-radius: var(--radius-full);
    font-size: var(--font-size-sm);
    color: var(--color-text-secondary);
    cursor: pointer;
    flex-shrink: 0;

    &.is-active {
      background: var(--color-text-primary);
      color: #fff;
      border-color: var(--color-text-primary);
    }
  }

  &__draft-btn {
    padding: 10px 18px;
    border: 1px solid var(--color-border);
    background: var(--color-card);
    color: var(--color-text-secondary);
    border-radius: var(--radius-full);
    font-size: var(--font-size-sm);
    cursor: pointer;
    flex-shrink: 0;

    &:hover:not(:disabled) {
      border-color: var(--color-text-tertiary);
      color: var(--color-text-primary);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__publish-btn {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 20px;
    border: none;
    background: var(--color-primary);
    color: #fff;
    border-radius: var(--radius-full);
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-semibold);
    cursor: pointer;
    transition: all var(--transition-fast);

    &:hover:not(:disabled) {
      background: var(--color-primary-hover);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
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

// ── Responsive ──
@media (max-width: 767px) {
  .create-post-page {
    padding: 0 var(--spacing-sm);
  }

  .top-bar {
    padding: var(--spacing-sm) 0;

    &__mode-btn {
      display: none;
    }
  }

  .title-input {
    font-size: 24px;
  }

  .editor-body {
    padding-bottom: 100px;
  }

  .content-input {
    min-height: 300px;
    font-size: var(--font-size-base);
  }

  .meta-row__item {
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  .upload-zone {
    padding: var(--spacing-lg);
  }

  .uploaded-list {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }

  .bottom-bar {
    display: block;
  }
}
</style>
