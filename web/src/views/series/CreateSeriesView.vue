<template>
  <div class="create-series">
    <div class="create-series__card card-base">
      <h1 class="page-title">创建系列</h1>

      <el-form :model="form" label-position="top" :rules="rules" ref="formRef">
        <el-form-item label="系列标题" prop="title">
          <el-input v-model="form.title" maxlength="200" placeholder="给你的系列起个名字" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简单介绍一下这个系列..." />
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="form.coverUrl" placeholder="可选，输入图片链接" />
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">创建系列</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { seriesApi } from '@/api/modules/series'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = ref({
  title: '',
  description: '',
  coverUrl: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入系列标题', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await seriesApi.create(form.value)
    ElMessage.success('系列创建成功')
    router.replace(`/series/${res.data.id}`)
  } catch { /* handled */ }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.create-series {
  max-width: 680px;
  margin: 0 auto;
  padding: 0 var(--spacing-md);
}

.create-series__card {
  padding: var(--spacing-2xl);
}

.page-title {
  font-size: 24px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xl);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-lg);
}

@media (max-width: 767px) {
  .create-series__card { padding: var(--spacing-lg); }
}
</style>
