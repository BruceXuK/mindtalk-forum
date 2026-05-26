<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-logo">MindTalk</h1>
      <p class="auth-subtitle">创建你的账号</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称（选填）" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" size="large" style="width: 100%" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-footer">
        <span>已有账号？<router-link to="/login">立即登录</router-link></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', email: '', nickname: '', password: '', confirmPassword: '' })

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) callback(new Error('两次密码输入不一致'))
  else callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度 3-30 个字符', trigger: 'blur' }
  ],
  email: [{ required: true, type: 'email', message: '请输入有效的邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度 6-30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userApi.register({ username: form.username, password: form.password, email: form.email, nickname: form.nickname || undefined })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch { /* error handled */ }
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
  width: 420px; max-width: 100%;
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
