<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ChatDotRound, Close, Promotion } from '@element-plus/icons-vue'
import { adminAiChatApi } from '@/api/adminAi'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  queryType?: string
  sources?: string[]
}

const visible = ref(false)
const input = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const listRef = ref<HTMLElement>()

const quickQuestions = [
  '今天心内科还有没有医生可以挂号？',
  '阿莫西林还剩多少库存？',
  '哪些药品库存低于10？',
  '今天有多少挂号？',
  '有哪些故障设备？',
]

function newId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
}

function openDialog() {
  visible.value = true
  if (messages.value.length === 0) {
    messages.value.push({
      id: newId(),
      role: 'assistant',
      content:
        '你好，我是管理助手。你可以用口语提问，例如「今天心内科还有没有医生可以挂号？」「布洛芬还剩多少？」我会根据系统真实数据回答。',
    })
  }
}

function closeDialog() {
  visible.value = false
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

async function sendQuestion(text?: string) {
  const question = (text ?? input.value).trim()
  if (!question || loading.value) return

  messages.value.push({ id: newId(), role: 'user', content: question })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const result = await adminAiChatApi(question)
    messages.value.push({
      id: newId(),
      role: 'assistant',
      content: result.answer,
      queryType: result.queryType,
      sources: result.sources,
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendQuestion()
  }
}
</script>

<template>
  <div class="ai-fab-root">
    <transition name="fab-pop">
      <button v-if="!visible" class="fab-btn" type="button" aria-label="AI 查询" @click="openDialog">
        <el-icon :size="24"><ChatDotRound /></el-icon>
        <span class="fab-label">AI 查询</span>
      </button>
    </transition>

    <transition name="panel-slide">
      <div v-if="visible" class="chat-panel">
        <div class="panel-header">
          <div class="panel-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 智能查询</span>
          </div>
          <el-button :icon="Close" circle size="small" @click="closeDialog" />
        </div>

        <div ref="listRef" class="message-list">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="msg.role"
          >
            <div class="bubble">
              <p class="bubble-text">{{ msg.content }}</p>
              <div v-if="msg.sources?.length" class="meta">
                数据来源：{{ msg.sources.join('、') }}
              </div>
            </div>
          </div>
          <div v-if="loading" class="message-row assistant">
            <div class="bubble loading-bubble">
              <span class="dot" /><span class="dot" /><span class="dot" />
            </div>
          </div>
        </div>

        <div class="quick-row">
          <el-tag
            v-for="q in quickQuestions"
            :key="q"
            class="quick-tag"
            effect="plain"
            round
            @click="sendQuestion(q)"
          >
            {{ q }}
          </el-tag>
        </div>

        <div class="input-row">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 3 }"
            placeholder="例如：布洛芬还剩多少？"
            :disabled="loading"
            @keydown="onKeydown"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            circle
            :loading="loading"
            :disabled="!input.trim()"
            @click="sendQuestion()"
          />
        </div>

        <p class="disclaimer">回答仅基于系统数据库查询结果，不会编造或联网检索</p>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ai-fab-root {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 3000;
}

.fab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.45);
  transition: transform 0.2s, box-shadow 0.2s;
}

.fab-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(64, 158, 255, 0.5);
}

.fab-label {
  font-size: 14px;
  font-weight: 600;
}

.chat-panel {
  width: 380px;
  max-width: calc(100vw - 32px);
  height: 560px;
  max-height: calc(100vh - 48px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.panel-header :deep(.el-button) {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.4);
  background: rgba(255, 255, 255, 0.12);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 88%;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.message-row.user .bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
}

.bubble-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.meta {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
  font-size: 11px;
  color: #909399;
}

.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 48px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: blink 1.2s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px 0;
  max-height: 72px;
  overflow-y: auto;
}

.quick-tag {
  cursor: pointer;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px;
}

.input-row :deep(.el-textarea__inner) {
  resize: none;
}

.disclaimer {
  margin: 0;
  padding: 0 12px 10px;
  font-size: 11px;
  color: #909399;
  text-align: center;
}

.fab-pop-enter-active,
.fab-pop-leave-active,
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.25s ease;
}

.fab-pop-enter-from,
.fab-pop-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
</style>
