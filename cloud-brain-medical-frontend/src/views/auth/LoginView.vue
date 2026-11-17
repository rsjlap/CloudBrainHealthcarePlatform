<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { loginApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { UserRole } from '@/types/api'
import {
  getLastRecentUsername,
  loadRecentUsers,
  removeRecentUser,
  roleLabels,
  saveRecentUser,
  type RecentUser,
} from '@/utils/recentUsers'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const recentUsers = ref<RecentUser[]>([])

const form = reactive({
  username: '',
  password: '',
})

function defaultHome(role: UserRole) {
  if (role === 'ADMIN') return '/admin/dashboard'
  if (role === 'DOCTOR') return '/doctor'
  if (role === 'INSPECTOR') return '/doctor/inspector'
  if (role === 'PHARMACIST') return '/doctor/pharmacist'
  return '/'
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function refreshRecentUsers() {
  recentUsers.value = loadRecentUsers()
}

function onRemoveRecent(username: string, event: Event) {
  event.stopPropagation()
  removeRecentUser(username)
  refreshRecentUsers()
  if (form.username === username) {
    form.username = recentUsers.value[0]?.username ?? ''
  }
}

async function onSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await loginApi(form)
    if (data.role === 'PATIENT') {
      ElMessage.warning('患者端已下线，请使用医生/管理员/医技人员账号登录')
      return
    }
    userStore.setLogin(data)
    await userStore.fetchProfile()
    saveRecentUser({
      username: data.username,
      realName: data.realName,
      role: data.role,
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || defaultHome(data.role)
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.reason === 'patient-disabled') {
    ElMessage.warning('患者端已下线，请使用医生/管理员/医技人员账号登录')
  }
  refreshRecentUsers()
  form.username = getLastRecentUsername()
})
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2>用户登录</h2>
      <p class="subtitle">东软智慧云脑诊疗平台</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent="onSubmit">
        <el-form-item label="用户名" prop="username">
          <el-select
            v-model="form.username"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择历史账号或输入用户名"
            style="width: 100%"
          >
            <el-option
              v-for="user in recentUsers"
              :key="user.username"
              :label="user.username"
              :value="user.username"
            >
              <div class="user-option">
                <div class="user-option-main">
                  <span class="username">{{ user.username }}</span>
                  <span v-if="user.realName" class="real-name">{{ user.realName }}</span>
                  <el-tag v-if="user.role" size="small" type="info" class="role-tag">
                    {{ roleLabels[user.role] }}
                  </el-tag>
                </div>
                <el-button
                  type="danger"
                  link
                  :icon="Delete"
                  class="remove-btn"
                  @click="onRemoveRecent(user.username, $event)"
                />
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f5f7fa 100%);
}

.auth-card {
  width: 460px;
  padding: 8px;
}

h2 {
  margin: 0;
  text-align: center;
}

.subtitle {
  text-align: center;
  color: #909399;
  margin: 8px 0 24px;
}

.footer {
  text-align: center;
  color: #909399;
}

.user-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;
}

.user-option-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.username {
  font-weight: 500;
  color: #303133;
}

.real-name {
  color: #909399;
  font-size: 12px;
}

.role-tag {
  flex-shrink: 0;
}

.remove-btn {
  flex-shrink: 0;
  padding: 0 4px;
}
</style>
