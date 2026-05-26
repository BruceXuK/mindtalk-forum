<template>
  <DefaultLayout v-if="!isAdminRoute" />
  <AdminLayout v-else />
  <el-backtop :right="40" :bottom="40" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { useHotkeys } from '@/composables/useHotkeys'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
useHotkeys()
</script>
