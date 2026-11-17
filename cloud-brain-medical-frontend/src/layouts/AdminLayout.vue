<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis,
  Monitor,
  User,
  Calendar,
  Goods,
  List,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AdminAiChatFab from '@/components/AdminAiChatFab.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/users')) {
    return '/admin/users/patients'
  }
  return route.path
})

const menuItems = [
  { index: '/admin/dashboard', title: '数据概览', icon: DataAnalysis },
  { index: '/admin/users/patients', title: '用户管理', icon: User },
  { index: '/admin/drugs', title: '药品管理', icon: Goods },
  { index: '/admin/registrations', title: '挂号记录', icon: List },
  { index: '/admin/schedules', title: '排班管理', icon: Calendar },
  { index: '/admin/equipment', title: '设备管理', icon: Monitor },
]

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
  <el-container class="admin-layout">
    <el-aside width="240px" class="aside">
      <div class="brand" @click="router.push('/admin/dashboard')">
        <span class="logo">云脑</span>
        <span class="title">东软智慧云脑诊疗平台</span>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">东软智慧云脑诊疗平台</span>
        </div>
        <div class="header-right">
          <span class="user">{{ userStore.userInfo?.realName }}</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <AdminAiChatFab />
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.aside {
  background: #001529;
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  cursor: pointer;
}

.logo {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 8px;
}

.title {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  line-height: 1.3;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.75);
}

.menu :deep(.el-menu-item.is-active) {
  background: #1890ff !important;
  color: #fff;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  color: #606266;
  font-size: 14px;
}

.main {
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}
</style>
