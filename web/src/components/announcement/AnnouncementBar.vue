<template>
  <div v-if="visibleAnnouncements.length > 0" class="announcement-bar">
    <div
      v-for="a in visibleAnnouncements"
      :key="a.id"
      class="announcement-item"
      :class="'level-' + a.level.toLowerCase()"
    >
      <div class="announcement-left">
        <span class="announcement-badge">{{ levelBadge(a.level) }}</span>
        <span class="announcement-summary">{{ a.summary || a.title }}</span>
        <button
          v-if="a.content && a.content !== a.summary"
          class="announcement-toggle"
          @click="toggleExpand(a.id)"
        >
          {{ expanded.has(a.id) ? '收起' : '详情' }}
        </button>
      </div>
      <div class="announcement-right">
        <button class="announcement-close" @click="dismiss(a.id)" title="关闭">x</button>
      </div>
      <div v-if="expanded.has(a.id) && a.content" class="announcement-content">
        {{ a.content }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { announcementApi } from '@/api/modules/announcement'
import { dictApi, type DictItem } from '@/api/modules/dict'
import type { AnnouncementVO } from '@/types'

const announcements = ref<AnnouncementVO[]>([])
const dismissed = ref<Set<number>>(new Set())
const expanded = ref<Set<number>>(new Set())
const levelMap = ref<Record<string, string>>({})

const visibleAnnouncements = computed(() =>
  announcements.value.filter(a => !dismissed.value.has(a.id))
)

onMounted(async () => {
  try {
    const stored = localStorage.getItem('announcement_dismissed')
    if (stored) {
      JSON.parse(stored).forEach((id: number) => dismissed.value.add(id))
    }
    const [dictRes, annRes] = await Promise.all([
      dictApi.getItems('ANNOUNCE_LEVEL'),
      announcementApi.getActiveList()
    ]);
    (dictRes.data || []).forEach((d: DictItem) => {
      levelMap.value[d.itemKey] = d.itemValue
    })
    announcements.value = annRes.data || []
  } catch { /* ignore */ }
})

function dismiss(id: number) {
  dismissed.value.add(id)
  localStorage.setItem('announcement_dismissed', JSON.stringify([...dismissed.value]))
}

function toggleExpand(id: number) {
  if (expanded.value.has(id)) {
    expanded.value.delete(id)
  } else {
    expanded.value.add(id)
  }
}

function levelBadge(level: string) {
  return levelMap.value[level] || '公告'
}
</script>

<style lang="scss" scoped>
.announcement-bar {
  border-radius: var(--radius-md, 12px);
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
}

.announcement-item {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  padding: 10px 16px;
  font-size: 14px;
  position: relative;

  &.level-info {
    background: #EFF6FF;
    border-left: 4px solid #2563EB;
    color: #1E40AF;
    html.dark & { background: #1E3A5F; color: #BFDBFE; }
  }
  &.level-warning {
    background: #FFFBEB;
    border-left: 4px solid #F59E0B;
    color: #92400E;
    html.dark & { background: #5C4A1E; color: #FDE68A; }
  }
  &.level-important {
    background: #FEF2F2;
    border-left: 4px solid #EF4444;
    color: #991B1B;
    html.dark & { background: #5F1E1E; color: #FECACA; }
  }
}

.announcement-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.announcement-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.announcement-summary {
  line-height: 1.5;
}

.announcement-toggle {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  text-decoration: underline;
  opacity: 0.7;
  padding: 0;
  color: inherit;
}

.announcement-right {
  flex-shrink: 0;
  margin-left: 8px;
}

.announcement-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  opacity: 0.5;
  padding: 0 4px;
  color: inherit;
  &:hover { opacity: 1; }
}

.announcement-content {
  width: 100%;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
