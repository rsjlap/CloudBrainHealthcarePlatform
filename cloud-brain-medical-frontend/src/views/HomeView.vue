<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<template>
  <div class="home">
    <el-card class="hero" shadow="never">
      <h1>东软智慧云脑诊疗平台</h1>
      <p>Spring Boot 3 + Vue 3 前后端分离 · AI 赋能智慧医疗</p>
      <div class="actions">
        <el-button v-if="!userStore.isLoggedIn" type="primary" size="large" @click="router.push('/login')">
          立即登录
        </el-button>
        <el-button v-if="userStore.isDoctor" type="primary" size="large" @click="router.push('/doctor')">
          医生工作台
        </el-button>
        <el-button v-if="userStore.isAdmin" type="primary" size="large" @click="router.push('/admin/dashboard')">
          管理后台
        </el-button>
        <el-button v-if="userStore.isInspector" type="primary" size="large" @click="router.push('/doctor/inspector')">
          检查工作台
        </el-button>
        <el-button v-if="userStore.isPharmacist" type="primary" size="large" @click="router.push('/doctor/pharmacist')">
          发药工作台
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="features">
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>医生工作台</h3>
          <p>接诊排队、电子病历、AI 辅助书写与知识图谱融合。</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>检查与处方</h3>
          <p>检查申请、处方开具与 AI+KG 审核，全流程数字化。</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <h3>管理后台</h3>
          <p>用户、排班、设备统计等运营管理功能。</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.hero {
  text-align: center;
  padding: 48px 24px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ecf5ff, #fff);
}

.hero h1 {
  margin: 0 0 12px;
  font-size: 28px;
}

.hero p {
  color: #606266;
  margin-bottom: 24px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.features {
  margin-bottom: 24px;
}

.features h3 {
  margin-top: 0;
}
</style>
