<template>
  <div class="settings-page">
    <div class="settings-card card-base">
      <h2 class="settings-title">通知设置</h2>
      <p class="settings-desc">选择你想接收的通知类型</p>

      <div class="settings-list" v-loading="loading">
        <div v-for="item in settings" :key="item.notifyType" class="setting-row">
          <div class="setting-row__info">
            <span class="setting-row__label">{{ item.label }}</span>
          </div>
          <el-switch
            :model-value="item.enabled"
            @change="(val: boolean) => toggleSetting(item.notifyType, val)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { notificationSettingsApi } from '@/api/modules/notificationSettings'
import type { NotificationSettingVO } from '@/types'
import { ElMessage } from 'element-plus'

const settings = ref<NotificationSettingVO[]>([])
const loading = ref(false)

onMounted(loadSettings)

async function loadSettings() {
  loading.value = true
  try {
    const res = await notificationSettingsApi.getList()
    settings.value = res.data
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function toggleSetting(notifyType: string, enabled: boolean) {
  try {
    await notificationSettingsApi.update(notifyType, enabled)
    const item = settings.value.find(s => s.notifyType === notifyType)
    if (item) item.enabled = enabled
    ElMessage.success(enabled ? '已开启' : '已关闭')
  } catch { /* handled */ }
}
</script>

<style lang="scss" scoped>
.settings-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

.settings-card {
  padding: var(--spacing-2xl);
}

.settings-title {
  font-size: 22px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.settings-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--color-divider);
}

.settings-list {
  display: flex;
  flex-direction: column;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-divider);

  &:last-child { border-bottom: none; }

  &__info {
    display: flex;
    flex-direction: column;
  }

  &__label {
    font-size: var(--font-size-base);
    color: var(--color-text-primary);
    font-weight: var(--font-weight-medium);
  }
}

@media (max-width: 767px) {
  .settings-card { padding: var(--spacing-lg); }
}
</style>
