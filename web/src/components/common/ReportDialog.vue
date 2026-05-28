<template>
  <el-dialog v-model="visible" title="举报内容" width="440px" @close="reset">
    <el-form label-position="top">
      <el-form-item label="举报类型">
        <span class="report-target-type">{{ targetTypeLabel }}</span>
      </el-form-item>
      <el-form-item label="举报原因">
        <el-radio-group v-model="reasonKey" class="reason-group">
          <el-radio v-for="r in reasons" :key="r.itemKey" :value="r.itemKey" class="reason-item">
            {{ r.itemValue }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="详细描述（可选）">
        <el-input
          v-model="description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请补充更多信息..."
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { reportApi } from '@/api/modules/report'
import { dictApi, type DictItem } from '@/api/modules/dict'

const props = defineProps<{
  modelValue: boolean
  targetType: string
  targetId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const reasons = ref<DictItem[]>([])
const targetTypeLabel = ref('')
const reasonKey = ref('')
const description = ref('')
const submitting = ref(false)

watch(() => props.modelValue, async (val) => {
  if (val) {
    if (reasons.value.length === 0) {
      await loadDicts()
    }
    reasonKey.value = reasons.value[0]?.itemKey || ''
    description.value = ''
  }
})

async function loadDicts() {
  try {
    const [reasonsRes, typesRes] = await Promise.all([
      dictApi.getItems('REPORT_REASON'),
      dictApi.getItems('REPORT_TARGET_TYPE')
    ])
    reasons.value = reasonsRes.data || []
    const typeItem = (typesRes.data || []).find(t => t.itemKey === props.targetType)
    targetTypeLabel.value = typeItem?.itemValue || props.targetType
  } catch { /* handled */ }
}

function reset() {
  reasonKey.value = reasons.value[0]?.itemKey || ''
  description.value = ''
}

async function submit() {
  submitting.value = true
  try {
    await reportApi.createReport({
      targetType: props.targetType,
      targetId: props.targetId,
      reason: reasonKey.value,
      description: description.value || undefined
    })
    ElMessage.success('举报已提交，我们会尽快处理')
    visible.value = false
  } catch { /* handled */ }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.report-target-type {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}
.reason-group {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.reason-item {
  margin-right: 0 !important;
  width: calc(50% - 2px);
}
</style>
