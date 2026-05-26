<template>
  <div v-for="n in count" :key="n" class="skeleton-card">
    <div class="sk-line sk-author"></div>
    <div class="sk-line sk-title"></div>
    <div class="sk-line sk-text"></div>
    <div class="sk-line sk-text short"></div>
    <div class="sk-footer">
      <div class="sk-line sk-tag"></div>
      <div class="sk-line sk-stats"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ count?: number }>()
</script>

<style lang="scss" scoped>
.skeleton-card {
  padding: var(--spacing-lg) 0;
  border-bottom: 1px solid var(--color-divider);

  &:first-child {
    padding-top: 0;
  }
}

.sk-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-divider) 50%, var(--color-bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: var(--spacing-sm);
}

.sk-author { width: 120px; height: 16px; }
.sk-title { width: 65%; height: 20px; margin-bottom: var(--spacing-md); }
.sk-text { width: 85%; }
.sk-text.short { width: 35%; }

.sk-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-md);
}

.sk-tag { width: 60px; margin-bottom: 0; }
.sk-stats { width: 100px; margin-bottom: 0; }
</style>
