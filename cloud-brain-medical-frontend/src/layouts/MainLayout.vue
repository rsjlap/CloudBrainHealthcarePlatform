<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

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
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="router.push('/')">
        <span class="logo">云脑</span>
        <span class="title">东软智慧云脑诊疗平台</span>
      </div>
      <el-menu mode="horizontal" :ellipsis="false" :default-active="activeMenu" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item v-if="userStore.isInspector" index="/doctor/inspector">待检查</el-menu-item>
        <el-menu-item v-if="userStore.isInspector" index="/doctor/inspector/history">历史记录</el-menu-item>
        <el-menu-item v-if="userStore.isPharmacist" index="/doctor/pharmacist">待发药</el-menu-item>
        <el-menu-item v-if="userStore.isPharmacist" index="/doctor/pharmacist/history">发药记录</el-menu-item>
        <el-menu-item v-if="userStore.isLoggedIn" index="/profile">个人中心</el-menu-item>
      </el-menu>
      <div class="actions">
        <template v-if="userStore.isLoggedIn">
          <span class="user">{{ userStore.userInfo?.realName }}</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" link @click="router.push('/login')">登录</el-button>
        </template>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo {
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 8px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header .el-menu {
  flex: 1;
  border-bottom: none;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user {
  color: #606266;
  font-size: 14px;
}

.main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
</style>
