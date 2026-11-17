<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useDoctorWebSocket } from '@/composables/useDoctorWebSocket'

const router = useRouter()
const userStore = useUserStore()

useDoctorWebSocket()

async function handleLogout() {
  userStore.logout()
  try {
    await router.replace({ name: 'login' })
  } catch {
    window.location.assign('/login')
  }
}
</script>

<template>
  <el-container class="doctor-layout">
    <el-header class="header">
      <div class="brand" @click="router.push('/doctor')">
        <span class="logo">云脑</span>
        <span class="title">医生工作站</span>
      </div>
      <div class="actions">
        <el-button
          type="primary"
          link
          :class="{ active: router.currentRoute.value.name === 'doctor-history' }"
          @click="router.push('/doctor/history')"
        >
          我的接诊病历
        </el-button>
        <el-button
          type="primary"
          link
          :class="{ active: router.currentRoute.value.name === 'doctor' }"
          @click="router.push('/doctor')"
        >
          工作台
        </el-button>
        <span
          class="user user-link"
          :class="{ active: router.currentRoute.value.name === 'doctor-profile' }"
          @click="router.push('/doctor/profile')"
        >
          {{ userStore.userInfo?.realName }} 医生
        </span>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.doctor-layout {
  min-height: 100vh;
  background: #eef1f6;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  padding: 0 20px;
  height: 52px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.logo {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  color: #606266;
  font-size: 14px;
}

.user-link {
  cursor: pointer;
  transition: color 0.2s;
}

.user-link:hover,
.user-link.active {
  color: #409eff;
}

.actions .el-button.active {
  font-weight: 600;
}

.main {
  padding: 0;
  height: calc(100vh - 52px);
  overflow: hidden;
}
</style>
