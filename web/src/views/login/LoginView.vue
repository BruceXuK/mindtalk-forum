<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-logo">MindTalk</h1>
      <p class="auth-subtitle">登录你的账号</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名 / 邮箱" prop="account">
          <el-input v-model="form.account" placeholder="请输入用户名或邮箱" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" size="large" style="width: 100%" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        <span>还没有账号？<router-link to="/register">立即注册</router-link></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '@/composables/useAuth'
import { useUserStore } from '@/stores/modules/user'
import { userApi } from '@/api/modules/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ account: '', password: '' })

const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function parseRoleFromToken(token: string): string {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.role || 'USER'
  } catch { return 'USER' }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await userApi.login(form)
    const roles = [parseRoleFromToken(res.data.accessToken)]
    login(res.data.accessToken, res.data.refreshToken, roles)
    userStore.setUserInfo(res.data.user)
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch { /* error handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style lang="scss" scoped>
.auth-page {
  display: flex; justify-content: center; align-items: center;
  min-height: calc(100vh - var(--header-height) - var(--module-gap) * 4);
  padding: var(--spacing-lg);
}
.auth-card {
  width: 400px; max-width: 100%;
  background: var(--color-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-lg);
  border: var(--border-width) solid var(--color-border);
}
.auth-logo {
  text-align: center; font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold);
  color: var(--color-primary); margin-bottom: var(--spacing-xs);
}
.auth-subtitle {
  text-align: center; font-size: var(--font-size-sm); color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
}
.auth-footer {
  text-align: center; font-size: var(--font-size-sm); color: var(--color-text-tertiary);
  a { color: var(--color-primary); font-weight: var(--font-weight-medium); }
}
@media (max-width: 767px) {
  .auth-card { padding: var(--spacing-lg); }
}
</style>
