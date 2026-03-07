<template>
  <div class="chat-container">
    <el-button class="chat-fab" type="primary" circle size="large" @click="toggleChat">
      <el-icon size="20"><Service /></el-icon>
    </el-button>

    <transition name="el-zoom-in-bottom">
      <el-card v-show="isOpen" class="chat-window" shadow="always">
        <template #header>
          <div class="chat-header">
            <div class="chat-title">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI 非遗助手</span>
            </div>
            <div class="header-actions">
              <el-tooltip content="清空当前对话">
                <el-icon class="action-icon" @click="clearHistory"><Delete /></el-icon>
              </el-tooltip>
              <el-icon class="action-icon" @click="isOpen = false"><Close /></el-icon>
            </div>
          </div>
        </template>

        <div ref="messageListRef" class="message-list">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-item"
            :class="message.role === 'user' ? 'message-user' : 'message-ai'"
          >
            <el-avatar :size="30" :src="message.role === 'user' ? userAvatar : aiAvatar" class="msg-avatar" />
            <div class="msg-bubble">
              <div v-if="message.role === 'assistant'" class="ai-name">AI 非遗助手</div>
              <div class="msg-content" v-html="formatContent(message.content)"></div>
            </div>
          </div>

          <div v-if="isLoading" class="message-item message-ai">
            <el-avatar :size="30" :src="aiAvatar" class="msg-avatar" />
            <div class="msg-bubble">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入与非遗项目、技艺、传承人相关的问题，Enter 发送，Shift+Enter 换行"
            @keydown.enter.prevent="handleEnter"
          />
          <el-button class="send-btn" type="primary" @click="sendMessage" :loading="isLoading">发送</el-button>
        </div>
      </el-card>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ChatDotRound, Close, Delete, Service } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { DEFAULT_AVATAR_URL, toServerUrl } from '@/utils/url'

type MessageRole = 'user' | 'assistant'

interface ChatMessage {
  role: MessageRole
  content: string
}

const aiAvatar = 'https://cdn-icons-png.flaticon.com/512/4712/4712027.png'
const isOpen = ref(false)
const isLoading = ref(false)
const inputContent = ref('')
const messageListRef = ref<HTMLElement | null>(null)
const messages = ref<ChatMessage[]>([
  {
    role: 'assistant',
    content: '你好，我可以帮你查询非遗项目、传承人、文化背景和项目亮点。',
  },
])

const userInfo = computed(() => {
  const userStr = sessionStorage.getItem('user')
  if (!userStr) {
    return null
  }

  try {
    return JSON.parse(userStr)
  } catch {
    return null
  }
})

const userAvatar = computed(() => {
  if (userInfo.value?.avatarUrl) {
    return toServerUrl(userInfo.value.avatarUrl)
  }
  return DEFAULT_AVATAR_URL
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const toggleChat = () => {
  isOpen.value = !isOpen.value
  scrollToBottom()
}

const clearHistory = () => {
  messages.value = [
    {
      role: 'assistant',
      content: '对话已清空。你可以继续询问非遗项目、保护级别、历史背景或传承人信息。',
    },
  ]
}

const handleEnter = (event: KeyboardEvent) => {
  if (!event.shiftKey) {
    sendMessage()
  }
}

const formatContent = (content: string) => content.replace(/\n/g, '<br/>')

const sendMessage = async () => {
  const text = inputContent.value.trim()
  if (!text) {
    return
  }

  if (!userInfo.value?.id) {
    ElMessage.warning('请先登录后再使用 AI 助手')
    return
  }

  messages.value.push({ role: 'user', content: text })
  inputContent.value = ''
  scrollToBottom()
  isLoading.value = true

  try {
    const res = await request.post('/chat/send', {
      message: text,
      userId: userInfo.value.id,
    })

    const reply = res.data.data?.reply || '暂时没有获取到 AI 回复。'
    messages.value.push({ role: 'assistant', content: reply })
  } catch (error: any) {
    const message = error?.response?.data?.msg || 'AI 服务暂时不可用，请稍后重试。'
    messages.value.push({ role: 'assistant', content: message })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 2000;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.28);
}

.chat-window {
  position: fixed;
  right: 28px;
  bottom: 90px;
  width: 380px;
  height: 560px;
  z-index: 2001;
  display: flex;
  flex-direction: column;
}

.chat-window :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-icon {
  cursor: pointer;
  color: #909399;
}

.action-icon:hover {
  color: #409eff;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 88%;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-ai {
  align-self: flex-start;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  word-break: break-word;
  line-height: 1.6;
}

.message-user .msg-bubble {
  background: #409eff;
  color: #fff;
}

.ai-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.chat-input-area {
  position: relative;
  padding: 14px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.send-btn {
  position: absolute;
  right: 22px;
  bottom: 22px;
}

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin: 0 2px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0);
  }

  40% {
    transform: scale(1);
  }
}
</style>
