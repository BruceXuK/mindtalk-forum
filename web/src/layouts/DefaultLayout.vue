<template>
  <div class="app-layout">
    <AppHeader />

    <div class="app-body">
      <AppSidebar class="app-left-sidebar" />
      <main class="app-content">
        <router-view v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
      <AppRightSidebar class="app-right-sidebar" />
    </div>

    <MobileDrawer />
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppRightSidebar from '@/components/layout/AppRightSidebar.vue'
import MobileDrawer from '@/components/layout/MobileDrawer.vue'
</script>

<style lang="scss" scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.app-body {
  flex: 1;
  max-width: var(--max-width);
  width: 100%;
  margin: 0 auto;
  padding: var(--module-gap);
  display: flex;
  gap: var(--module-gap);
}

.app-left-sidebar { flex-shrink: 0; }
.app-content { flex: 1; min-width: 0; }
.app-right-sidebar { flex-shrink: 0; }

@media (max-width: 1279px) {
  .app-right-sidebar { display: none; }
}

@media (max-width: 1023px) {
  .app-left-sidebar { display: none; }
}

@media (max-width: 767px) {
  .app-body { padding: var(--spacing-md); flex-direction: column; }
}
</style>
