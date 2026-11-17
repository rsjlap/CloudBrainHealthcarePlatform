<script setup lang="ts">
defineProps<{
  title: string
  aiAvailable?: boolean
  kgAvailable?: boolean
  steps: string[]
}>()
</script>

<template>
  <div class="fusion-strip">
    <div class="fusion-title">{{ title }}</div>
    <div class="fusion-tags">
      <el-tag :type="aiAvailable ? 'success' : 'info'" size="small">DeepSeek</el-tag>
      <el-tag v-if="kgAvailable" type="warning" size="small">知识图谱</el-tag>
      <el-tag v-else type="info" size="small" effect="plain">知识图谱未接入</el-tag>
    </div>
    <div class="fusion-steps">
      <span v-for="(step, idx) in steps" :key="step" class="fusion-step">
        <span class="step-dot" :class="{ done: idx < steps.length - 1 || aiAvailable }">{{ idx + 1 }}</span>
        <span class="step-text">{{ step }}</span>
        <span v-if="idx < steps.length - 1" class="step-arrow">→</span>
      </span>
    </div>
  </div>
</template>

<style scoped>
.fusion-strip {
  margin-bottom: 10px;
  padding: 10px;
  background: linear-gradient(135deg, #f5f7ff 0%, #faf5ff 100%);
  border: 1px solid #e4e7f5;
  border-radius: 6px;
}

.fusion-title {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.fusion-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.fusion-steps {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 4px;
  font-size: 11px;
  color: #606266;
}

.fusion-step {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.step-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #dcdfe6;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.step-dot.done {
  background: #667eea;
}

.step-arrow {
  color: #c0c4cc;
  margin: 0 2px;
}
</style>
