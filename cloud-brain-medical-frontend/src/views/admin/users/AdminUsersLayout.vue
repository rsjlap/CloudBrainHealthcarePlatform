<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeTab = computed(() => route.path)
</script>

<template>
  <div class="users-layout">
    <el-card shadow="never" class="nav-card">
      <el-menu mode="horizontal" router :default-active="activeTab" class="sub-menu">
        <el-menu-item index="/admin/users/patients">患者管理</el-menu-item>
        <el-menu-item index="/admin/users/staff">工作人员管理</el-menu-item>
      </el-menu>
    </el-card>
    <router-view />
  </div>
</template>

<style scoped>
.users-layout {
  padding: 8px 0;
}

.nav-card {
  margin-bottom: 16px;
}

.nav-card :deep(.el-card__body) {
  padding: 0 16px;
}

.sub-menu {
  border-bottom: none;
}
</style>
