<script setup lang="ts">
defineProps<{
  total: number
  page: number
  size: number
}>()

const emit = defineEmits<{
  'update:page': [number]
  'update:size': [number]
  change: []
}>()
</script>

<template>
  <div class="admin-pagination">
    <el-pagination
      :current-page="page"
      :page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="(p: number) => { emit('update:page', p); emit('change') }"
      @size-change="(s: number) => { emit('update:size', s); emit('update:page', 1); emit('change') }"
    />
  </div>
</template>

<style scoped>
.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
