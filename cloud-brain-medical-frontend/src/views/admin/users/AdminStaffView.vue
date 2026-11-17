<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminStaffApi, updateAdminStaffApi, type AdminUser } from '@/api/admin'
import { adminDoctorDetailApi, updateAdminDoctorApi } from '@/api/adminDoctor'
import { listDepartmentsApi } from '@/api/department'
import AdminPagination from '@/components/AdminPagination.vue'
import type { Department, UserRole } from '@/types/api'

const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const staffList = ref<AdminUser[]>([])
const departments = ref<Department[]>([])
const dialogVisible = ref(false)
const editing = ref<AdminUser | null>(null)
const formRef = ref<FormInstance>()

const pagination = reactive({ page: 1, size: 10, total: 0 })

const filters = reactive({
  keyword: '',
  role: '' as UserRole | '',
})

const form = reactive({
  realName: '',
  phone: '',
  password: '',
  status: 1,
  departmentId: undefined as number | undefined,
  title: '',
  specialty: '',
  introduction: '',
  registrationFee: 0,
  isAvailable: 1,
})

const isDoctor = computed(() => editing.value?.role === 'DOCTOR')

const rules = computed<FormRules>(() => ({
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  ...(isDoctor.value
    ? { departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }] }
    : {}),
}))

const roleLabel: Record<UserRole, string> = {
  ADMIN: '管理员',
  DOCTOR: '医生',
  PATIENT: '患者',
  INSPECTOR: '检查医生',
  PHARMACIST: '药师',
}

const roleType: Record<UserRole, 'danger' | 'primary' | 'success' | 'warning' | 'info'> = {
  ADMIN: 'danger',
  DOCTOR: 'primary',
  PATIENT: 'success',
  INSPECTOR: 'warning',
  PHARMACIST: 'info',
}

const staffRoles: UserRole[] = ['ADMIN', 'DOCTOR', 'INSPECTOR', 'PHARMACIST']

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function resetForm() {
  form.realName = ''
  form.phone = ''
  form.password = ''
  form.status = 1
  form.departmentId = undefined
  form.title = ''
  form.specialty = ''
  form.introduction = ''
  form.registrationFee = 0
  form.isAvailable = 1
}

async function loadStaff() {
  loading.value = true
  try {
    const params: { role?: UserRole; keyword?: string; page: number; size: number } = {
      page: pagination.page,
      size: pagination.size,
    }
    if (filters.role) params.role = filters.role
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    const result = await adminStaffApi(params)
    staffList.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.role = ''
  pagination.page = 1
  loadStaff()
}

async function openEdit(row: AdminUser) {
  editing.value = row
  resetForm()
  dialogVisible.value = true
  detailLoading.value = true
  try {
    if (row.role === 'DOCTOR') {
      const detail = await adminDoctorDetailApi(row.id)
      form.realName = detail.realName
      form.phone = detail.phone || ''
      form.status = detail.status
      form.departmentId = detail.departmentId
      form.title = detail.title || ''
      form.specialty = detail.specialty || ''
      form.introduction = detail.introduction || ''
      form.registrationFee = detail.registrationFee ?? 0
      form.isAvailable = detail.isAvailable ?? 1
    } else {
      form.realName = row.realName
      form.phone = row.phone || ''
      form.status = row.status
    }
  } finally {
    detailLoading.value = false
  }
}

async function onSubmit() {
  await formRef.value?.validate()
  if (!editing.value) return
  saving.value = true
  try {
    if (editing.value.role === 'DOCTOR') {
      await updateAdminDoctorApi(editing.value.id, {
        realName: form.realName,
        phone: form.phone || undefined,
        password: form.password || undefined,
        departmentId: form.departmentId,
        title: form.title || undefined,
        specialty: form.specialty || undefined,
        introduction: form.introduction || undefined,
        registrationFee: form.registrationFee,
        isAvailable: form.isAvailable,
        status: form.status,
      })
    } else {
      await updateAdminStaffApi(editing.value.id, {
        realName: form.realName,
        phone: form.phone || undefined,
        password: form.password || undefined,
        status: form.status,
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadStaff()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  departments.value = await listDepartmentsApi()
  await loadStaff()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>工作人员列表</span>
          <el-button type="primary" :loading="loading" @click="loadStaff">刷新</el-button>
        </div>
      </template>

      <el-alert
        title="管理医生、检查医生、药师及管理员账号，不含患者账号。点击编辑可修改基本信息；医生账号还可设置科室、挂号费等。"
        type="info"
        :closable="false"
        show-icon
        class="tip"
      />

      <el-form inline class="filters">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="姓名/用户名/手机号"
            clearable
            style="width: 180px"
            @keyup.enter="loadStaff"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.role" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="role in staffRoles"
              :key="role"
              :label="roleLabel[role]"
              :value="role"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { pagination.page = 1; loadStaff() }">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="staffList" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }: { row: AdminUser }">
            <el-tag :type="roleType[row.role]" size="small">{{ roleLabel[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: AdminUser }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }: { row: AdminUser }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }: { row: AdminUser }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <AdminPagination
        v-model:page="pagination.page"
        v-model:size="pagination.size"
        :total="pagination.total"
        @change="loadStaff"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`编辑${editing ? roleLabel[editing.role] : '工作人员'}`"
      width="520px"
      destroy-on-close
    >
      <div v-loading="detailLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item v-if="editing" label="用户名">
            <span>{{ editing.username }}</span>
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="留空表示不修改" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不修改请留空" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">正常</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>

          <template v-if="isDoctor">
            <el-divider content-position="left">医生信息</el-divider>
            <el-form-item label="科室" prop="departmentId">
              <el-select v-model="form.departmentId" placeholder="选择科室" style="width: 100%">
                <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="职称">
              <el-input v-model="form.title" placeholder="如：主任医师" />
            </el-form-item>
            <el-form-item label="专长">
              <el-input v-model="form.specialty" placeholder="如：心血管疾病" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="挂号费">
              <el-input-number v-model="form.registrationFee" :min="0" :precision="2" :step="1" />
            </el-form-item>
            <el-form-item label="出诊状态">
              <el-radio-group v-model="form.isAvailable">
                <el-radio :value="1">可出诊</el-radio>
                <el-radio :value="0">暂停出诊</el-radio>
              </el-radio-group>
            </el-form-item>
          </template>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tip {
  margin-bottom: 16px;
}

.filters {
  margin-bottom: 16px;
}
</style>
