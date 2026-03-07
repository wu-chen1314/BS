<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <span>Chat Sessions</span>
        <el-button type="primary" size="small" @click="createSession">New</el-button>
      </div>
      <div class="session-list" v-loading="loadingSessions">
        <div v-for="item in chatList" :key="item.id" :class="['session-item', { active: item.id === activeChatId }]" @click="selectSession(item)">
          <div class="session-title">{{ item.title || 'New Chat' }}</div>
          <div class="session-meta">
            <span>{{ formatTime(item.updatedAt) }}</span>
            <el-button link type="danger" @click.stop="deleteSession(item.id)">Delete</el-button>
          </div>
        </div>
        <el-empty v-if="!loadingSessions && chatList.length === 0" description="No chat sessions" :image-size="90" />
      </div>
    </div>

    <div class="chat-main">
      <div class="chat-header">AI Heritage Assistant</div>
      <div class="message-list" ref="messageListRef" v-loading="loadingMessages">
        <div v-for="item in messages" :key="item.id" :class="['message-row', item.role]">
          <div class="message-bubble">
            <div class="message-role">{{ item.role === 'user' ? 'You' : 'AI' }}</div>
            <div class="message-text" v-html="formatMessage(item.content)"></div>
            <div class="message-time">{{ formatTime(item.timestamp) }}</div>
          </div>
        </div>
        <el-empty v-if="!loadingMessages && messages.length === 0" description="Start a new conversation" :image-size="100" />
      </div>
      <div class="composer">
        <el-input v-model="inputMessage" type="textarea" :rows="3" resize="none" placeholder="Ask something about intangible cultural heritage" @keyup.enter.exact.prevent="sendMessage" />
        <div class="composer-actions">
          <el-button @click="inputMessage = ''">Clear</el-button>
          <el-button type="primary" :loading="sending" @click="sendMessage">Send</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

interface ChatSession {
  id: number
  title?: string
  updatedAt?: string
}

interface MessageItem {
  id: number
  role: 'user' | 'assistant'
  content: string
  timestamp: string | Date
}

const user = ref<any>({})
const chatList = ref<ChatSession[]>([])
const activeChatId = ref<number | null>(null)
const messages = ref<MessageItem[]>([])
const inputMessage = ref('')
const sending = ref(false)
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const loadUser = () => {
  const raw = sessionStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : {}
}

const getUserId = () => user.value?.id || user.value?.userId

const loadSessions = async () => {
  const userId = getUserId()
  if (!userId) return
  loadingSessions.value = true
  try {
    const res = await request.get('/chat/sessions', { params: { userId, limit: 20 } })
    chatList.value = res.data.data || []
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load chat sessions')
  } finally {
    loadingSessions.value = false
  }
}

const loadHistory = async (chatId?: number) => {
  const userId = getUserId()
  if (!userId) return
  loadingMessages.value = true
  try {
    const res = await request.get('/chat/history', { params: { userId, chatId, limit: 50 } })
    messages.value = (res.data.data || []).map((item: any, index: number) => ({
      id: Date.now() + index,
      role: item.role === 'user' ? 'user' : 'assistant',
      content: item.content,
      timestamp: item.timestamp,
    }))
    await scrollToBottom()
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to load messages')
  } finally {
    loadingMessages.value = false
  }
}

const createSession = async () => {
  const userId = getUserId()
  if (!userId) return ElMessage.warning('Please log in first')
  try {
    const res = await request.post('/chat/sessions', { userId, title: 'New Chat' })
    if (res.data.code === 200) {
      await loadSessions()
      activeChatId.value = res.data.data.id
      messages.value = []
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('Failed to create session')
  }
}

const selectSession = async (item: ChatSession) => {
  activeChatId.value = item.id
  await loadHistory(item.id)
}

const deleteSession = async (chatId: number) => {
  try {
    await ElMessageBox.confirm('Delete this chat session?', 'Confirm', { type: 'warning' })
    const res = await request.delete(`/chat/sessions/${chatId}`, { params: { userId: getUserId() } })
    if (res.data.code === 200) {
      if (activeChatId.value === chatId) {
        activeChatId.value = null
        messages.value = []
      }
      await loadSessions()
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.response?.data?.msg || 'Failed to delete session')
    }
  }
}

const sendMessage = async () => {
  const userId = getUserId()
  const text = inputMessage.value.trim()
  if (!userId) return ElMessage.warning('Please log in first')
  if (!text) return

  sending.value = true
  const localMessage: MessageItem = {
    id: Date.now(),
    role: 'user',
    content: text,
    timestamp: new Date(),
  }
  messages.value.push(localMessage)
  inputMessage.value = ''
  await scrollToBottom()

  try {
    const res = await request.post('/chat/send', {
      userId,
      message: text,
      chatId: activeChatId.value,
    }, { timeout: 60000 })

    if (!activeChatId.value && res.data.data?.chatId) {
      activeChatId.value = res.data.data.chatId
    }

    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: res.data.data?.reply || 'No reply',
      timestamp: new Date(),
    })
    await loadSessions()
    await scrollToBottom()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.msg || 'Failed to send message')
  } finally {
    sending.value = false
  }
}

const formatMessage = (content: string) => (content || '').replace(/\n/g, '<br>')

const formatTime = (value?: string | Date) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', { hour12: false })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(async () => {
  loadUser()
  await loadSessions()
})
</script>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  min-height: calc(100vh - 120px);
  padding: 20px;
}

.chat-sidebar,
.chat-main {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.chat-sidebar {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header,
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #edf2f7;
  font-weight: 600;
}

.session-list {
  flex: 1;
  overflow: auto;
  padding: 8px;
}

.session-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
}

.session-item.active {
  background: #f3f6fb;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.chat-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f8fafc;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 14px;
  border-radius: 12px;
  background: #ffffff;
}

.message-row.user .message-bubble {
  background: #dbeafe;
}

.message-role {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.message-text {
  line-height: 1.6;
  color: #0f172a;
}

.message-time {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.composer {
  padding: 16px;
  border-top: 1px solid #edf2f7;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

@media (max-width: 960px) {
  .chat-page {
    grid-template-columns: 1fr;
  }
}
</style>