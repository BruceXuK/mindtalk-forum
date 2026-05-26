<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <router-link to="/admin" class="admin-logo">管理后台</router-link>
      </div>
      <el-menu
        :default-active="activeMenu"
        :router="true"
        :background-color="config.admin.sidebarBg"
        :text-color="config.admin.sidebarTextColor"
        :active-text-color="config.admin.sidebarActiveColor"
      >
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/posts">
          <el-icon><Document /></el-icon>
          <span>帖子审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/reports">
          <el-icon><Warning /></el-icon>
          <span>举报处理</span>
        </el-menu-item>
        <el-menu-item index="/admin/roles">
          <el-icon><Lock /></el-icon>
          <span>权限管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>
        <el-menu-item index="/admin/sensitive-words">
          <el-icon><Warning /></el-icon>
          <span>敏感词</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button text type="primary" @click="goHome">
          <el-icon><Back /></el-icon> 返回前台
        </el-button>
      </div>
    </aside>
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppConfig } from '@/composables/useAppConfig'
import { User, Document, ChatDotRound, Warning, Lock, DataAnalysis, Back } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const config = useAppConfig()

const activeMenu = computed(() => route.path)

function goHome() {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

.admin-sidebar {
  width: v-bind('config.admin.sidebarWidth + "px"');
  background: #1E293B;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  html.dark & {
    background: #0F172A;
  }
}

.sidebar-header {
  padding: 20px;
  text-align: center;

  .admin-logo {
    color: #F1F5F9;
    font-size: 18px;
    font-weight: 700;
  }
}

.el-menu {
  border-right: none;
  flex: 1;
}

.sidebar-footer {
  padding: 12px;
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .el-button {
    color: #94A3B8;
  }
}

.admin-main {
  flex: 1;
  padding: 20px;
  background: var(--color-bg);
  overflow-y: auto;
}

@media (max-width: 767px) {
  .admin-sidebar {
    display: none;
  }

  .admin-main {
    padding: var(--spacing-md);
  }
}
</style>
