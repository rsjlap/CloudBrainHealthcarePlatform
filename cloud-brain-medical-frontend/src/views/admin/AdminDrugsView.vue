<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AdminPagination from '@/components/AdminPagination.vue'
import {
  adminDrugsApi,
  createAdminDrugApi,
  updateAdminDrugApi,
  type AdminDrug,
} from '@/api/adminDrug'

const loading = ref(false)
const saving = ref(false)
const drugs = ref<AdminDrug[]>([])
const dialogVisible = ref(false)
const editing = ref<AdminDrug | null>(null)
const formRef = ref<FormInstance>()

const pagination = reactive({ page: 1, size: 10, total: 0 })

const filters = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const form = reactive({
  drugCode: '',
  drugName: '',
  specification: '',
  unit: '',
  unitPrice: 0,
  manufacturer: '',
  contraindication: '',
  status: 1,
  stockQuantity: 0,
})

const rules: FormRules = {
  drugCode: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
}

function resetForm() {
  form.drugCode = ''
  form.drugName = ''
  form.specification = ''
  form.unit = ''
  form.unitPrice = 0
  form.manufacturer = ''
  form.contraindication = ''
  form.status = 1
  form.stockQuantity = 0
}

async function loadDrugs() {
  loading.value = true
  try {
    const result = await adminDrugsApi({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
    })
    drugs.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = undefined
  pagination.page = 1
  loadDrugs()
}

function openCreate() {
  editing.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminDrug) {
  editing.value = row
  form.drugCode = row.drugCode
  form.drugName = row.drugName
  form.specification = row.specification || ''
  form.unit = row.unit || ''
  form.unitPrice = row.unitPrice ?? 0
  form.manufacturer = row.manufacturer || ''
  form.contraindication = row.contraindication || ''
  form.status = row.status
  form.stockQuantity = row.stockQuantity
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      drugCode: form.drugCode,
      drugName: form.drugName,
      specification: form.specification || undefined,
      unit: form.unit || undefined,
      unitPrice: form.unitPrice,
      manufacturer: form.manufacturer || undefined,
      contraindication: form.contraindication || undefined,
      status: form.status,
      stockQuantity: form.stockQuantity,
    }
    if (editing.value) {
      await updateAdminDrugApi(editing.value.id, payload)
      ElMessage.success('药品已更新')
    } else {
      await createAdminDrugApi(payload)
      ElMessage.success('药品已新增')
    }
    dialogVisible.value = false
    await loadDrugs()
  } finally {
    saving.value = false
  }
}

onMounted(loadDrugs)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>药品管理</span>
          <el-button type="primary" @click="openCreate">新增药品</el-button>
        </div>
      </template>

      <el-form inline class="filters">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="编码/名称"
            clearable
            style="width: 180px"
            @keyup.enter="loadDrugs"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="在售" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="loadDrugs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="drugs" stripe>
        <el-table-column prop="drugCode" label="编码" width="110" />
        <el-table-column prop="drugName" label="药品名称" min-width="140" />
        <el-table-column prop="specification" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="单价" width="90" align="right">
          <template #default="{ row }: { row: AdminDrug }">
            {{ row.unitPrice != null ? `¥${row.unitPrice}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="库存" width="80" align="center">
          <template #default="{ row }: { row: AdminDrug }">
            <span :class="{ 'low-stock': row.stockQuantity <= 10 }">{{ row.stockQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="manufacturer" label="生产厂家" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }: { row: AdminDrug }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }: { row: AdminDrug }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <AdminPagination
        v-model:page="pagination.page"
        v-model:size="pagination.size"
        :total="pagination.total"
        @change="loadDrugs"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑药品' : '新增药品'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编码" prop="drugCode">
          <el-input v-model="form.drugCode" />
        </el-form-item>
        <el-form-item label="名称" prop="drugName">
          <el-input v-model="form.drugName" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stockQuantity" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生产厂家">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="禁忌">
          <el-input v-model="form.contraindication" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">在售</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
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

.filters {
  margin-bottom: 16px;
}

.low-stock {
  color: #f56c6c;
  font-weight: 600;
}
</style>
