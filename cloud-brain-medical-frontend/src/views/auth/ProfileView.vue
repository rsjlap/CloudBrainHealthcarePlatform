<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)

onMounted(async () => {
  if (userStore.isLoggedIn) {
    loading.value = true
    try {
      await userStore.fetchProfile()
    } finally {
      loading.value = false
    }
  }
})
</script>

<template>
  <el-card v-loading="loading" shadow="never">
    <template #header>
      <span>个人中心</span>
    </template>
    <el-descriptions v-if="userStore.userInfo" :column="1" border>
      <el-descriptions-item label="用户ID">{{ userStore.userInfo.userId }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
      <el-descriptions-item label="真实姓名">{{ userStore.userInfo.realName }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ userStore.userInfo.phone }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ userStore.userInfo.role }}</el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="请先登录" />
  </el-card>
</template>
