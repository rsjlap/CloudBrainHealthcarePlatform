<script setup lang="ts">
import { computed } from 'vue'
import { isKgPlaceholder, parseKgReference, splitKgAnswerLines } from '@/utils/kgReference'

const props = defineProps<{
  content?: string
}>()

const parsed = computed(() => parseKgReference(props.content))
const hasValidAnswer = computed(() => !!parsed.value.answer && !isKgPlaceholder(parsed.value.answer))
const answerLines = computed(() => splitKgAnswerLines(parsed.value.answer))
const showQuestion = computed(() => !!parsed.value.question)
</script>

<template>
  <div v-if="hasValidAnswer" class="kg-ref-view">
    <div class="kg-ref-label">图谱回复</div>
    <div v-if="parsed.meta" class="kg-ref-meta">{{ parsed.meta }}</div>
    <ul v-if="answerLines.length > 1" class="kg-ref-list">
      <li v-for="(line, index) in answerLines" :key="index">{{ line }}</li>
    </ul>
    <p v-else class="kg-ref-answer">{{ parsed.answer }}</p>

    <el-collapse v-if="showQuestion" class="kg-ref-question">
      <el-collapse-item title="校验问题（展开查看）" name="question">
        <p class="kg-ref-question-text">{{ parsed.question }}</p>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<style scoped>
.kg-ref-view {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.kg-ref-label {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #909399;
}

.kg-ref-meta {
  margin-bottom: 8px;
  padding: 6px 10px;
  font-size: 12px;
  color: #606266;
  background: #f4f4f5;
  border-radius: 4px;
}

.kg-ref-answer {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.kg-ref-list {
  margin: 0;
  padding-left: 18px;
}

.kg-ref-list li {
  margin-bottom: 6px;
}

.kg-ref-list li:last-child {
  margin-bottom: 0;
}

.kg-ref-question {
  margin-top: 10px;
  border: none;
}

.kg-ref-question :deep(.el-collapse-item__header) {
  height: 32px;
  font-size: 12px;
  color: #909399;
  background: transparent;
  border: none;
}

.kg-ref-question :deep(.el-collapse-item__wrap) {
  border: none;
}

.kg-ref-question :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.kg-ref-question-text {
  margin: 0;
  font-size: 12px;
  color: #909399;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
