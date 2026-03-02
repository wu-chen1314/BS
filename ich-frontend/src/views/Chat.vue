<template>
    <div class="chat-container">
        <!-- 动态背景装饰 -->
        <div class="background-decoration">
            <div class="decoration-circle circle-1"></div>
            <div class="decoration-circle circle-2"></div>
            <div class="decoration-circle circle-3"></div>
            <div class="decoration-blob"></div>
        </div>

        <div class="chat-layout">
            <!-- 左侧聊天记录列表 -->
            <div class="chat-sidebar">
                <div class="sidebar-header">
                    <div class="header-left">
                        <el-icon class="sidebar-icon">
                            <ChatDotRound />
                        </el-icon>
                        <h3 class="sidebar-title">聊天记录</h3>
                    </div>
                    <div class="header-actions">
                        <el-button v-if="!isBatchMode" type="success" size="small" :icon="Edit" @click="toggleBatchMode"
                            class="batch-mode-btn" title="批量管理" />
                        <template v-else>
                            <el-button type="danger" size="small" :icon="Delete" @click="batchDelete"
                                :disabled="selectedChats.length === 0" class="batch-delete-btn">
                                删除 ({{ selectedChats.length }})
                            </el-button>
                            <el-button type="default" size="small" @click="toggleBatchMode" class="cancel-batch-btn">
                                取消
                            </el-button>
                        </template>
                    </div>
                    <el-button v-if="!isBatchMode" type="primary" size="small" :icon="Plus" circle @click="createNewChat"
                        class="new-chat-btn" />
                </div>

                <div class="chat-list">
                    <div v-if="chatList.length === 0" class="empty-chat">
                        <el-empty description="暂无聊天记录，点击左上角 + 创建新对话" :image-size="60" />
                    </div>
                    <div v-else v-for="chat in chatList" :key="chat.id"
                        :class="['chat-item', { active: activeChatId === chat.id, 'selected': selectedChats.includes(chat.id) }]"
                        @click="isBatchMode ? toggleChatSelection(chat.id) : selectChat(chat)">
                        <div class="chat-item-left">
                            <div class="chat-item-checkbox" v-if="isBatchMode">
                                <el-checkbox :model-value="selectedChats.includes(chat.id)"
                                    @click.stop="toggleChatSelection(chat.id)" />
                            </div>
                            <div class="chat-item-icon" v-else>
                                <el-icon>
                                    <ChatLineRound />
                                </el-icon>
                            </div>
                            <div class="chat-item-content">
                                <div class="chat-item-title">{{ chat.title || '新对话' }}</div>
                                <div class="chat-item-preview">{{ formatPreview(chat.lastMessage) }}</div>
                            </div>
                        </div>
                        <div class="chat-item-right">
                            <div class="chat-item-time">{{ formatChatTime(chat.updatedAt) }}</div>
                            <el-button v-if="!isBatchMode" type="danger" size="small" text :icon="Delete"
                                @click.stop="deleteChat(chat.id)" class="delete-btn" />
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右侧主聊天区域 -->
            <div class="chat-main">
                <!-- 顶部标题栏 -->
                <div class="page-header">
                    <div class="header-content">
                        <div class="header-left">
                            <div class="ai-avatar-wrapper">
                                <div class="avatar-container">
                                    <el-avatar :size="56" :src="aiAvatar" class="header-avatar floating" />
                                    <div class="avatar-status">
                                        <div class="status-dot online"></div>
                                        <div class="status-ring"></div>
                                    </div>
                                </div>
                                <div class="avatar-glow"></div>
                            </div>
                            <div class="header-text">
                                <h2 class="page-title">
                                    <span class="title-gradient">AI 非遗知识助手</span>
                                    <span class="title-badge">
                                        <span class="badge-dot"></span>
                                        在线
                                    </span>
                                </h2>
                                <p class="page-subtitle">与 AI 对话，探索非遗文化的奥秘</p>
                            </div>
                        </div>
                        <div class="header-right">
                            <div class="user-info">
                                <div class="user-avatar-wrapper">
                                    <el-avatar :size="48" :src="getUserAvatar" class="user-avatar floating" />
                                    <div class="user-status">
                                        <span class="status-indicator"></span>
                                    </div>
                                </div>
                                <div class="user-details">
                                    <span class="user-name">{{ userInfo.nickname || '用户' }}</span>
                                    <span class="user-role">
                                        <el-tag :type="userInfo.role === 'admin' ? 'danger' : 'success'" size="small"
                                            effect="plain">
                                            {{ userInfo.role === 'admin' ? '管理员' : '在线' }}
                                        </el-tag>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 聊天内容区 -->
                <div class="chat-content">
                    <div class="chat-history" ref="chatHistory">
                        <!-- 欢迎消息 -->
                        <div v-if="messages.length === 0" class="welcome-message">
                            <div class="welcome-hero">
                                <div class="welcome-icon">
                                    <div class="icon-container">
                                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                            <path
                                                d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
                                                fill="currentColor" />
                                        </svg>
                                    </div>
                                </div>
                                <h1 class="welcome-title">欢迎使用非遗知识助手</h1>
                                <p class="welcome-text">我是你的「非遗小百科」，可以回答你关于非物质文化遗产的各种问题</p>
                            </div>
                            <div class="welcome-suggestions">
                                <div class="suggestion-section">
                                    <h3 class="section-title">💡 你可以这样问</h3>
                                    <div class="suggestion-grid">
                                        <div class="suggestion-card" @click="sendSuggestion('什么是非物质文化遗产？')">
                                            <div class="card-icon">📚</div>
                                            <div class="card-content">
                                                <h4>什么是非物质文化遗产？</h4>
                                                <p>了解非遗的基本概念和定义</p>
                                            </div>
                                        </div>
                                        <div class="suggestion-card" @click="sendSuggestion('中国有哪些著名的非遗项目？')">
                                            <div class="card-icon">🏮</div>
                                            <div class="card-content">
                                                <h4>中国有哪些著名的非遗项目？</h4>
                                                <p>探索中国丰富的非遗文化</p>
                                            </div>
                                        </div>
                                        <div class="suggestion-card" @click="sendSuggestion('非遗如何保护和传承？')">
                                            <div class="card-icon">🛡️</div>
                                            <div class="card-content">
                                                <h4>非遗如何保护和传承？</h4>
                                                <p>了解非遗保护的方法和意义</p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- 聊天消息列表 -->
                        <div v-for="(message, index) in messages" :key="index"
                            :class="['message-item', message.role === 'user' ? 'user-message' : (message.role === 'system' ? 'system-message' : 'ai-message')]">
                            <div class="message-avatar">
                                <el-avatar :size="44"
                                    :src="message.role === 'user' ? getUserAvatar : (message.role === 'system' ? '' : aiAvatar)" />
                                <div class="avatar-badge">{{ message.role === 'user' ? '我' : (message.role === 'system'
                                    ? '系统' : 'AI') }}</div>
                            </div>
                            <div class="message-content">
                                <!-- AI 思考状态 -->
                                <div v-if="message.isThinking" class="thinking-bubble">
                                    <div class="wave-dots">
                                        <span></span><span></span><span></span><span></span><span></span>
                                    </div>
                                    <span class="thinking-text">AI 正在思考中，可能需要 30-60 秒...</span>
                                    <span class="thinking-tip">（AI 正在查询专业知识库，请耐心等待）</span>
                                </div>
                                <!-- 普通消息 -->
                                <template v-else>
                                    <div class="message-bubble">
                                        <div class="message-text" v-html="formatMessage(message.content)"></div>
                                    </div>
                                    <!-- 消息操作栏（仅 AI 消息显示） -->
                                    <div v-if="message.role === 'assistant'" class="message-actions">
                                        <el-button size="small" text @click="copyMessage(message.content)">
                                            <el-icon>
                                                <DocumentCopy />
                                            </el-icon>
                                            复制
                                        </el-button>
                                        <el-button size="small" text @click="regenerateResponse(index)">
                                            <el-icon>
                                                <Refresh />
                                            </el-icon>
                                            重新生成
                                        </el-button>
                                        <el-button size="small" text @click="rateMessage(message.id, 'up')">
                                            <el-icon>
                                                <Star />
                                            </el-icon>
                                            点赞
                                        </el-button>
                                        <el-button size="small" text @click="rateMessage(message.id, 'down')">
                                            <el-icon>
                                                <Star />
                                            </el-icon>
                                            点踩
                                        </el-button>
                                    </div>
                                    <div class="message-meta">
                                        <span class="message-time">{{ formatTime(message.timestamp) }}</span>
                                        <span v-if="message.role === 'user'"
                                            :class="['message-status', message.status || '']">
                                            {{ message.status === 'sending' ? '发送中...' : (message.status === 'error' ?
                                                '发送失败' : '已发送') }}
                                        </span>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </div>

                    <!-- 消息输入区 -->
                    <div class="chat-input-container">
                        <!-- 快捷问题卡片 -->
                        <div class="quick-questions">
                            <div class="questions-scroll">
                                <div v-for="item in quickQuestions" :key="item.id" class="question-chip"
                                    @click="sendSuggestion(item.question)">
                                    <span class="chip-emoji">{{ item.emoji }}</span>
                                    <span class="chip-text">{{ item.question }}</span>
                                </div>
                            </div>
                        </div>

                        <div class="input-wrapper">
                            <div class="input-header">
                                <span class="input-label">
                                    <el-icon>
                                        <ChatLineRound />
                                    </el-icon>
                                    输入您的问题
                                </span>
                                <span class="input-hint">按 Enter 发送，Shift + Enter 换行</span>
                            </div>
                            <div class="input-body">
                                <el-input v-model="inputMessage" placeholder="例如：什么是非物质文化遗产？"
                                    :disabled="isThinking || isSending" @keyup.enter.exact="sendMessage" resize="none"
                                    type="textarea" :rows="3" class="message-input"
                                    :class="{ 'has-content': inputMessage.trim() }" @input="adjustTextareaHeight"
                                    ref="messageInput" />
                                <div class="input-actions">
                                    <div class="action-buttons">
                                        <el-button @click="clearInput"
                                            :disabled="!inputMessage.trim() || (isThinking || isSending)"
                                            class="clear-btn">
                                            <el-icon>
                                                <Delete />
                                            </el-icon>
                                            清空
                                        </el-button>
                                        <el-button type="primary" @click="sendMessage" :loading="isThinking"
                                            :disabled="!inputMessage.trim() || (isThinking || isSending)"
                                            class="send-button">
                                            <el-icon>
                                                <ChatLineRound />
                                            </el-icon>
                                            发送消息
                                        </el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatLineRound, Delete, Plus, ChatDotRound, Star, Refresh, DocumentCopy, Edit } from '@element-plus/icons-vue'
import { marked } from 'marked'

// 类型定义
interface Message {
    id: number
    role: 'user' | 'assistant' | 'system'
    content: string
    timestamp: Date | string
    status?: 'sending' | 'sent' | 'error'
    isThinking?: boolean
}

interface ChatSession {
    id: number
    title: string
    lastMessage?: string
    updatedAt: string
    messages?: Message[]
}

interface QuickQuestion {
    id: number
    question: string
    emoji: string
}

// 配置常量
const CACHE_DURATION = 60000 // 缓存时间：1分钟
const MAX_RETRIES = 3 // 最大重试次数
// API_BASE_URL 已移除，使用 request 模块

// 状态管理
const messages = ref<Message[]>([])
const inputMessage = ref('')
const chatHistory = ref<HTMLElement>()
const chatList = ref<ChatSession[]>([])
const activeChatId = ref<number | null>(null)
const isSending = ref(false)
const isThinking = ref(false)
const retryCount = ref(0)
const conversationContext = ref<any[]>([])
const isBatchMode = ref(false) // 批量管理模式
const selectedChats = ref<number[]>([]) // 选中的聊天 ID 列表

// 缓存管理
const chatListCache = ref<any>(null)
const lastCacheTime = ref<number>(0)

// 快捷问题
const quickQuestions: QuickQuestion[] = [
    { id: 1, question: '什么是非物质文化遗产？', emoji: '📚' },
    { id: 2, question: '中国有哪些著名的非遗项目？', emoji: '🏮' },
    { id: 3, question: '非遗如何保护和传承？', emoji: '🛡️' },
    { id: 4, question: '京剧是什么时候被列为非遗的？', emoji: '🎭' },
    { id: 5, question: '中医针灸的特点是什么？', emoji: '🌿' },
    { id: 6, question: '中国剪纸艺术有哪些流派？', emoji: '✂️' }
]

// 资源配置
const aiAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=friendly%20Chinese%20cultural%20heritage%20AI%20assistant%20avatar%2C%20traditional%20elements%2C%20warm%20colors%2C%20professional%20looking%2C%20digital%20art&image_size=square'

// 用户信息管理
const userInfo = ref<any>({
    role: 'user',
    nickname: '',
    username: '',
    avatarUrl: ''
})

const getUserAvatar = computed(() => {
    if (userInfo.value.avatarUrl) {
        return userInfo.value.avatarUrl.startsWith('http')
            ? userInfo.value.avatarUrl
            : `http://localhost:8080${userInfo.value.avatarUrl}`
    }
    return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
})

// 工具函数
const generateId = () => {
    return Date.now() + Math.floor(Math.random() * 10000)
}



const formatChatTime = (timestamp: string) => {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))

    if (days === 0) {
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    if (days === 1) return '昨天'
    if (days < 7) return `${days}天前`
    return date.toLocaleDateString('zh-CN')
}

const formatPreview = (text?: string) => {
    if (!text) return ''
    const preview = text.replace(/<br\/?>/g, ' ').replace(/<[^>]+>/g, '')
    return preview.length > 30 ? preview.substring(0, 30) + '...' : preview
}

const formatTime = (timestamp: any) => {
    if (!timestamp) return ''
    let date
    if (timestamp instanceof Date) {
        date = timestamp
    } else if (typeof timestamp === 'string') {
        date = new Date(timestamp)
    } else if (typeof timestamp === 'number') {
        date = new Date(timestamp)
    } else {
        return ''
    }
    if (isNaN(date.getTime())) return ''
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatMessage = (content: string) => {
    if (!content) return ''
    try {
        return marked.parse(content)
    } catch (error) {
        console.error('Markdown 解析失败', error)
        return content.replace(/\n/g, '<br>')
    }
}

// API 调用函数
const loadUserInfo = () => {
    const userStr = sessionStorage.getItem('user')
    console.log('loadUserInfo - userStr:', userStr)
    if (userStr) {
        try {
            userInfo.value = JSON.parse(userStr)
            console.log('loadUserInfo - userInfo:', userInfo.value)
        } catch (e) {
            console.error('用户信息解析失败', e)
            sessionStorage.removeItem('user')
        }
    } else {
        console.log('loadUserInfo - 未找到用户信息')
    }
}

const loadChatList = async () => {
    const userId = userInfo.value?.id || userInfo.value?.userId || 1
    console.log('loadChatList - userId:', userId)

    // 检查缓存（优化：如果有 activeChatId，不使用缓存）
    const now = Date.now()
    if (!activeChatId.value && chatListCache.value && (now - lastCacheTime.value) < CACHE_DURATION) {
        console.log('使用缓存的聊天列表')
        chatList.value = chatListCache.value
        return
    }

    try {
        const headers = getAuthHeaders()
        console.log('loadChatList - 请求API:', `/chat/sessions`)
        const res = await request.get('/chat/sessions', {
            params: { userId, limit: 20 }
        })
        console.log('loadChatList - API响应:', res.data)
        if (res.data.code === 200 && res.data.data) {
            chatList.value = res.data.data
            chatListCache.value = res.data.data
            lastCacheTime.value = now
            console.log('loadChatList - 聊天列表加载成功，数量:', res.data.data.length)
        }
    } catch (error) {
        console.error('加载聊天列表失败', error)
    }
}

const loadChatHistory = async (chatId?: number) => {
    const userId = userInfo.value?.id || userInfo.value?.userId || 1
    console.log('loadChatHistory - userId:', userId, 'chatId:', chatId)

    try {
        const headers = getAuthHeaders()
        console.log('loadChatHistory - 请求参数:', { userId, chatId, limit: 50 })

        const res = await request.get('/chat/history', {
            params: { userId, chatId, limit: 50 }
        })
        console.log('loadChatHistory - API响应:', res.data)
        if (res.data.code === 200 && res.data.data) {
            messages.value = res.data.data.map((item: any) => ({
                id: generateId(),
                role: item.role === 'user' ? 'user' : 'assistant',
                content: item.content || '',
                timestamp: item.timestamp,
                status: 'sent'
            }))
            console.log('loadChatHistory - 聊天历史加载成功，数量:', res.data.data.length)
            await nextTick()
            scrollToBottom()
        }
    } catch (error) {
        console.error('加载聊天历史失败', error)
    }
}

const createNewChat = async () => {
    console.log('创建新聊天')
    messages.value = []
    activeChatId.value = null
    inputMessage.value = ''

    const userId = userInfo.value?.id || userInfo.value?.userId
    if (!userId) {
        ElMessage.warning('用户未登录')
        return
    }

    try {
        const headers = getAuthHeaders()
        const res = await request.post('/chat/sessions', {
            userId,
            title: '新对话'
        })
        if (res.data.code === 200 && res.data.data) {
            const newChat: ChatSession = {
                id: res.data.data.id,
                title: res.data.data.title || '新对话',
                updatedAt: res.data.data.updatedAt || new Date().toISOString(),
                messages: []
            }
            chatList.value = [newChat, ...chatList.value]
            activeChatId.value = newChat.id
            chatListCache.value = null
            lastCacheTime.value = 0
            ElMessage.success('新会话已创建')
        }
    } catch (error) {
        console.error('创建新聊天失败', error)
        ElMessage.error('创建失败')
    }
}

const deleteChat = async (chatId: number) => {
    try {
        await ElMessageBox.confirm('确认删除该聊天记录吗？', '警告', {
            type: 'warning',
            confirmButtonText: '确定',
            cancelButtonText: '取消'
        })

        const userId = userInfo.value?.id || userInfo.value?.userId

        const res = await request.delete(`/chat/sessions/${chatId}`, {
            params: { userId }
        })

        if (res.data.code === 200) {
            chatList.value = chatList.value.filter(chat => chat.id !== chatId)
            if (activeChatId.value === chatId) {
                messages.value = []
                activeChatId.value = null
            }
            chatListCache.value = null
            lastCacheTime.value = 0
            ElMessage.success('删除成功')
        } else {
            ElMessage.error(res.data.msg || '删除失败')
        }
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('删除失败', error)
            ElMessage.error(error.response?.data?.msg || '删除失败')
        }
    }
}

const sendMessage = async (content?: any) => {
    console.log('sendMessage - 开始发送消息')
    const messageContent = typeof content === 'string' && content.trim() ? content.trim() : inputMessage.value.trim()
    console.log('sendMessage - 消息内容:', messageContent)
    let willRetry = false

    if (!messageContent) {
        ElMessage.warning('请输入消息')
        return
    }

    if (isSending.value || isThinking.value) {
        ElMessage.warning('AI 正在思考中，请稍候')
        return
    }

    const userId = userInfo.value?.id || userInfo.value?.userId || 1
    console.log('sendMessage - userId:', userId)
    isSending.value = true

    // 添加用户消息
    const userMessage: Message = {
        id: generateId(),
        role: 'user',
        content: messageContent,
        timestamp: new Date(),
        status: 'sending'
    }
    messages.value = [...messages.value, userMessage]
    inputMessage.value = ''

    await nextTick()
    scrollToBottom()

    // 显示 AI 思考状态
    isThinking.value = true
    const thinkingMessage: Message = {
        id: generateId(),
        role: 'assistant',
        content: '',
        timestamp: new Date(),
        isThinking: true
    }
    messages.value = [...messages.value, thinkingMessage]
    await nextTick()
    scrollToBottom()

    try {
        // 构建对话上下文
        const contextMessages = messages.value
            .filter(m => !m.isThinking && m.role !== 'system')
            .slice(-10)
            .map(m => ({
                role: m.role,
                content: m.content
            }))
        console.log('sendMessage - 上下文消息:', contextMessages)

        console.log('sendMessage - 请求 API:', '/chat/send')
        const res = await request.post('/chat/send', {
            userId,
            message: messageContent,
            chatId: activeChatId.value,
            context: contextMessages
        }, {
            timeout: 60000 // 增加超时时间到 60 秒
        })
        console.log('sendMessage - API响应:', res.data)

        if (res.data.code === 200 && res.data.data) {
            console.log('AI 响应数据:', res.data.data)

            // 移除思考状态消息
            messages.value = messages.value.filter(m => !m.isThinking)

            // 更新用户消息状态
            const userMsgIndex = messages.value.findIndex(m => m.id === userMessage.id)
            if (userMsgIndex !== -1) {
                messages.value[userMsgIndex].status = 'sent'
            }

            // 添加 AI 回复
            const aiContent = res.data.data.reply || res.data.data.content || '抱歉，AI 暂时没有回复'
            const aiMessage: Message = {
                id: generateId(),
                role: 'assistant',
                content: aiContent,
                timestamp: new Date(),
                status: 'sent'
            }
            messages.value = [...messages.value, aiMessage]

            // 更新上下文
            conversationContext.value = contextMessages

            if (res.data.data.chatId) {
                if (activeChatId.value !== res.data.data.chatId) {
                    activeChatId.value = res.data.data.chatId
                    lastCacheTime.value = 0 // Invalidate cache
                }
            }

            retryCount.value = 0
            // ElMessage.success('AI 回复成功') // 减少成功提示，避免干扰
        } else {
            throw new Error(res.data.msg || 'AI 回复失败')
        }
    } catch (error: any) {
        console.error('发送消息失败', error)

        // 移除思考状态消息
        messages.value = messages.value.filter(m => !m.isThinking)

        // 错误处理和重试机制
        if (retryCount.value < MAX_RETRIES) {
            willRetry = true
            retryCount.value++
            ElMessage.warning(`网络不稳定，正在重试 (${retryCount.value}/${MAX_RETRIES})...`)

            setTimeout(() => {
                messages.value = messages.value.filter(m => m.id !== userMessage.id)
                sendMessage(messageContent)
            }, 3000)
            return
        }

        // 更新用户消息状态
        const userMsgIndex = messages.value.findIndex(m => m.id === userMessage.id)
        if (userMsgIndex !== -1) {
            messages.value[userMsgIndex].status = 'error'
        }

        // 构建错误消息
        let errorMessageContent = '抱歉，AI 暂时无法回复'
        if (error.response?.data?.msg) {
            errorMessageContent = `抱歉，AI 暂时无法回复：${error.response.data.msg}`
        } else if (error.message) {
            if (error.message.includes('timeout')) {
                errorMessageContent = '抱歉，AI 响应超时（超过 60 秒），这是正常现象，建议：1.耐心等待 2.稍后重试 3.简化问题'
            } else if (error.message.includes('Network Error')) {
                errorMessageContent = '抱歉，网络连接失败，请检查网络后重试'
            } else if (error.message.includes('504') || error.message.includes('Gateway Timeout')) {
                errorMessageContent = '抱歉，AI 服务响应超时，请稍后重试'
            } else {
                errorMessageContent = `抱歉，AI 暂时无法回复：${error.message}`
            }
        } else {
            errorMessageContent = '抱歉，AI 暂时无法回复：未知错误'
        }

        // 添加错误消息
        const errorMessage: Message = {
            id: generateId(),
            role: 'system',
            content: errorMessageContent,
            timestamp: new Date(),
            status: 'error'
        }
        messages.value = [...messages.value, errorMessage]

        // 显示友好的错误提示
        if (error.message?.includes('timeout')) {
            ElMessage.error('发送失败，AI 响应超时，请稍后重试')
        } else if (error.message?.includes('Network Error')) {
            ElMessage.error('发送失败，网络连接失败，请检查网络后重试')
        } else {
            ElMessage.error('发送失败，请稍后重试')
        }
    } finally {
        if (!willRetry) {
            isThinking.value = false
            isSending.value = false
            retryCount.value = 0
            await nextTick()
            scrollToBottom()
            loadChatList()
        }
    }
}

// 辅助函数（保留以兼容其他代码调用，但不再使用）
const getAuthHeaders = (token?: string) => {
    // 使用 request 模块自动处理认证，此函数不再需要
    return {}
}

// 批量管理相关函数
const toggleBatchMode = () => {
    isBatchMode.value = !isBatchMode.value
    selectedChats.value = []
    console.log('批量管理模式:', isBatchMode.value ? '开启' : '关闭')
}

const toggleChatSelection = (chatId: number) => {
    const index = selectedChats.value.indexOf(chatId)
    if (index > -1) {
        selectedChats.value.splice(index, 1)
    } else {
        selectedChats.value.push(chatId)
    }
    console.log('选中的聊天:', selectedChats.value)
}

const batchDelete = async () => {
    if (selectedChats.value.length === 0) {
        ElMessage.warning('请选择要删除的聊天记录')
        return
    }

    try {
        await ElMessageBox.confirm(
            `确认删除选中的 ${selectedChats.value.length} 条聊天记录吗？删除后无法恢复！`,
            '警告',
            {
                type: 'warning',
                confirmButtonText: '确定',
                cancelButtonText: '取消'
            }
        )

        const userId = userInfo.value?.id || userInfo.value?.userId

        // 批量删除
        const deletePromises = selectedChats.value.map(chatId =>
            request.delete(`/chat/sessions/${chatId}`, {
                params: { userId }
            })
        )

        const results = await Promise.all(deletePromises)
        const successCount = results.filter(res => res.data.code === 200).length

        // 从列表中移除已删除的聊天
        chatList.value = chatList.value.filter(chat => !selectedChats.value.includes(chat.id))

        // 如果当前选中的聊天被删除，清空消息
        if (activeChatId.value && selectedChats.value.includes(activeChatId.value)) {
            messages.value = []
            activeChatId.value = null
        }

        // 清空选中状态
        selectedChats.value = []
        isBatchMode.value = false
        chatListCache.value = null
        lastCacheTime.value = 0

        ElMessage.success(`成功删除 ${successCount} 条聊天记录`)
    } catch (error: any) {
        if (error !== 'cancel') {
            console.error('批量删除失败', error)
            ElMessage.error('批量删除失败，请重试')
        }
    }
}

const selectChat = async (chat: ChatSession) => {
    console.log('选择聊天:', chat.id)
    activeChatId.value = chat.id
    messages.value = []
    await loadChatHistory(chat.id)
    await nextTick()
    scrollToBottom()
}

const regenerateResponse = async (messageIndex: number) => {
    if (messageIndex < 1) return

    const userMessage = messages.value[messageIndex - 1]
    if (userMessage.role !== 'user') return

    messages.value = messages.value.filter((_, idx) => idx !== messageIndex && idx !== messageIndex - 1)
    await sendMessage(userMessage.content)
}

const copyMessage = async (content: string) => {
    try {
        await navigator.clipboard.writeText(content)
        ElMessage.success('✅ 已复制')
    } catch (error) {
        ElMessage.error('复制失败')
    }
}

const rateMessage = (messageId: number | undefined, rating: 'up' | 'down') => {
    console.log(`消息 ${messageId} 被${rating === 'up' ? '点赞' : '点踩'}`)
    ElMessage.success('感谢反馈')
}

const sendSuggestion = (suggestion: string) => {
    console.log('发送建议:', suggestion)
    inputMessage.value = suggestion
    sendMessage()
}

const clearInput = () => {
    inputMessage.value = ''
}

const adjustTextareaHeight = () => {
    nextTick(() => {
        // 滚动聊天历史到底部
        if (chatHistory.value) {
            chatHistory.value.scrollTop = chatHistory.value.scrollHeight
        }
        // 调整输入框高度
        const textarea = document.querySelector('.message-input textarea')
        if (textarea) {
            textarea.style.height = 'auto'
            textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
        }
    })
}

const scrollToBottom = () => {
    nextTick(() => {
        if (chatHistory.value) {
            chatHistory.value.scrollTop = chatHistory.value.scrollHeight
        }
    })
}

// 监听页面隐藏，清理定时器
let refreshTimer: any = null

// 组件生命周期
onMounted(async () => {
    console.log('Chat 组件已挂载')
    loadUserInfo()
    await nextTick()

    try {
        console.log('onMounted - 开始加载聊天列表')
        await loadChatList()
        console.log('onMounted - 聊天列表加载完成，数量:', chatList.value.length)
        if (chatList.value.length > 0) {
            console.log('onMounted - 选择第一个聊天:', chatList.value[0].id)
            await selectChat(chatList.value[0])
        } else {
            console.log('onMounted - 无聊天记录，清空消息')
            messages.value = []
        }
        console.log('聊天数据加载完成')
    } catch (error) {
        console.error('初始化失败:', error)
        ElMessage.error('加载失败，请刷新页面重试')
    }
})

// 组件卸载时清理
import { onUnmounted } from 'vue'
onUnmounted(() => {
    // 清理可能的定时器
    if (refreshTimer) {
        clearInterval(refreshTimer)
    }
})
</script>

<style scoped>
/* ========== 容器和布局 ========== */
.chat-container {
    min-height: 100vh;
    padding: 20px 24px;
    background: linear-gradient(135deg, #f8fafc 0%, #e8f0f8 100%);
    position: relative;
    overflow: hidden;
}

.chat-layout {
    display: flex;
    gap: 20px;
    height: calc(100vh - 40px);
    position: relative;
    z-index: 1;
}

/* 响应式布局 */
@media (max-width: 768px) {
    .chat-container {
        padding: 10px;
    }

    .chat-layout {
        flex-direction: column;
        height: calc(100vh - 20px);
    }

    .chat-sidebar {
        width: 100% !important;
        height: 200px;
    }

    .chat-main {
        flex: 1;
    }

    .suggestion-grid {
        grid-template-columns: 1fr !important;
    }
}

/* ========== 背景装饰动画 ========== */
.background-decoration {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
}

.decoration-circle {
    position: absolute;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(196, 30, 58, 0.05) 0%, transparent 70%);
}

.circle-1 {
    width: 200px;
    height: 200px;
    top: 10%;
    left: 5%;
}

.circle-2 {
    width: 150px;
    height: 150px;
    top: 60%;
    right: 10%;
}

.circle-3 {
    width: 100px;
    height: 100px;
    bottom: 20%;
    left: 15%;
}

.decoration-blob {
    position: absolute;
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(66, 153, 225, 0.08) 0%, transparent 70%);
    border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
}

/* ========== 左侧边栏 ========== */
.chat-sidebar {
    width: 320px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.5);
}

.sidebar-header {
    padding: 20px;
    border-bottom: 1px solid #e8f0f8;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
}

.header-left {
    display: flex;
    align-items: center;
    gap: 10px;
}

.sidebar-icon {
    font-size: 20px;
    color: #dc2626;
}

.sidebar-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1e293b;
}

.new-chat-btn {
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

.chat-list {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
}

.empty-chat {
    text-align: center;
    padding: 40px 20px;
    color: #94a3b8;
}

.chat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    margin-bottom: 8px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1px solid transparent;
}

.chat-item:hover {
    background: linear-gradient(135deg, #fef2f2 0%, #fff1f2 100%);
    border-color: #fecaca;
    transform: translateX(4px);
}

.chat-item.active {
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    border-color: #fca5a5;
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.15);
}

.chat-item-left {
    display: flex;
    gap: 12px;
    flex: 1;
    min-width: 0;
}

.chat-item-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #dc2626;
    font-size: 20px;
    flex-shrink: 0;
}

.chat-item-content {
    flex: 1;
    min-width: 0;
}

.chat-item-title {
    font-weight: 600;
    color: #1e293b;
    margin-bottom: 4px;
    font-size: 15px;
}

.chat-item-preview {
    font-size: 13px;
    color: #64748b;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.chat-item-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
    margin-left: 12px;
}

.chat-item-time {
    font-size: 12px;
    color: #94a3b8;
}

.delete-btn {
    opacity: 0;
    transition: opacity 0.3s ease;
    color: #ef4444;
}

.chat-item:hover .delete-btn {
    opacity: 1;
}

.delete-btn:hover {
    background: rgba(239, 68, 68, 0.1);
}

/* ========== 主聊天区域 ========== */
.chat-main {
    flex: 1;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.5);
}

.page-header {
    padding: 20px 24px;
    border-bottom: 1px solid #e8f0f8;
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.ai-avatar-wrapper {
    position: relative;
    display: inline-block;
}

.avatar-container {
    position: relative;
    display: inline-block;
}

.header-avatar {
    border: 3px solid #fff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.floating {
    animation: floating 3s ease-in-out infinite;
}

.avatar-status {
    position: absolute;
    bottom: -2px;
    right: -2px;
    width: 16px;
    height: 16px;
}

.status-dot {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: #22c55e;
    border: 2px solid #fff;
    animation: pulse 2s ease-in-out infinite;
}

.status-ring {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 2px solid #22c55e;
    animation: ripple 2s ease-out infinite;
}

.avatar-glow {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 70px;
    height: 70px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(220, 38, 38, 0.2) 0%, transparent 70%);
    animation: glow 3s ease-in-out infinite;
    pointer-events: none;
}

.header-text {
    margin-left: 16px;
}

.page-title {
    margin: 0 0 6px 0;
    font-size: 22px;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 10px;
}

.title-gradient {
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.title-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    color: #dc2626;
}

.badge-dot {
    width: 6px;
    height: 6px;
    background: #22c55e;
    border-radius: 50%;
    animation: blink 2s ease-in-out infinite;
}

.page-subtitle {
    margin: 0;
    font-size: 14px;
    color: #64748b;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 16px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-radius: 12px;
    border: 1px solid #e2e8f0;
}

.user-avatar-wrapper {
    position: relative;
}

.user-status {
    position: absolute;
    bottom: -2px;
    right: -2px;
}

.status-indicator {
    width: 12px;
    height: 12px;
    background: #22c55e;
    border: 2px solid #fff;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(34, 197, 94, 0.4);
}

.user-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.user-name {
    font-weight: 600;
    color: #1e293b;
    font-size: 14px;
}

.user-role {
    font-size: 12px;
}

/* ========== 聊天内容区 ========== */
.chat-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chat-history {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
    background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

/* 欢迎消息 */
.welcome-message {
    text-align: center;
    padding: 40px 20px;
}

.welcome-hero {
    margin-bottom: 40px;
}

.welcome-icon {
    margin-bottom: 20px;
}

.icon-container {
    width: 80px;
    height: 80px;
    margin: 0 auto;
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #dc2626;
    font-size: 40px;
    box-shadow: 0 8px 24px rgba(220, 38, 38, 0.2);
    animation: pulse-glow 3s ease-in-out infinite;
}

.welcome-title {
    font-size: 32px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px 0;
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}

.welcome-text {
    font-size: 16px;
    color: #64748b;
    margin: 0;
}

.suggestion-section {
    margin-top: 32px;
}

.section-title {
    font-size: 18px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 20px 0;
}

.suggestion-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 16px;
    max-width: 900px;
    margin: 0 auto;
}

.suggestion-card {
    padding: 20px;
    background: linear-gradient(135deg, #ffffff 0%, #fef2f2 100%);
    border: 1px solid #fecaca;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    gap: 16px;
    align-items: flex-start;
    text-align: left;
}

.suggestion-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(220, 38, 38, 0.15);
    border-color: #fca5a5;
}

.card-icon {
    font-size: 32px;
    flex-shrink: 0;
}

.card-content h4 {
    margin: 0 0 6px 0;
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
}

.card-content p {
    margin: 0;
    font-size: 13px;
    color: #64748b;
}

/* 消息列表 */
.message-item {
    display: flex;
    gap: 16px;
    margin-bottom: 24px;
    animation: slideIn 0.3s ease-out;
}

.user-message {
    flex-direction: row-reverse;
}

.message-avatar {
    position: relative;
    flex-shrink: 0;
}

.avatar-badge {
    position: absolute;
    bottom: -4px;
    left: 50%;
    transform: translateX(-50%);
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    color: white;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 8px;
    font-weight: 600;
    white-space: nowrap;
    box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
}

.message-content {
    max-width: 60%;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.user-message .message-content {
    align-items: flex-end;
}

.message-bubble {
    padding: 14px 18px;
    border-radius: 16px;
    line-height: 1.6;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    word-wrap: break-word;
}

.ai-message .message-bubble {
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    border: 1px solid #e2e8f0;
    color: #1e293b;
}

.user-message .message-bubble {
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    color: white;
}

.message-text {
    font-size: 15px;
    line-height: 1.7;
}

.message-text :deep(p) {
    margin: 0 0 12px 0;
}

.message-text :deep(p:last-child) {
    margin-bottom: 0;
}

.message-text :deep(code) {
    background: rgba(0, 0, 0, 0.06);
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
}

.message-text :deep(pre) {
    background: #1e293b;
    color: #f8fafc;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;
}

.message-text :deep(pre code) {
    background: transparent;
    padding: 0;
    color: inherit;
}

.message-text :deep(ul),
.message-text :deep(ol) {
    margin: 12px 0;
    padding-left: 24px;
}

.message-text :deep(li) {
    margin: 6px 0;
}

.message-text :deep(blockquote) {
    border-left: 4px solid #dc2626;
    margin: 12px 0;
    padding: 8px 16px;
    background: #fef2f2;
    border-radius: 4px;
}

.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3) {
    margin: 16px 0 8px 0;
    font-weight: 600;
}

.message-text :deep(h1) {
    font-size: 24px;
}

.message-text :deep(h2) {
    font-size: 20px;
}

.message-text :deep(h3) {
    font-size: 18px;
}

.message-meta {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: #94a3b8;
    align-items: center;
}

.message-time {
    font-weight: 500;
}

.message-status {
    font-weight: 500;
    padding: 2px 8px;
    border-radius: 8px;
    background: rgba(34, 197, 94, 0.1);
    color: #22c55e;
}

.message-status.sending {
    background: rgba(59, 130, 246, 0.1);
    color: #3b82f6;
}

.message-status.error {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
}

/* 系统消息 */
.system-message .message-bubble {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    border: 1px solid #f59e0b;
    color: #92400e;
    max-width: 80%;
}

/* 思考状态 */
.thinking-bubble {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-radius: 16px;
    border: 1px solid #e2e8f0;
}

.thinking-text {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
}

/* 消息操作栏 */
.message-actions {
    display: flex;
    gap: 8px;
    margin-top: 8px;
    opacity: 0;
    transition: opacity 0.3s ease;
}

.message-item:hover .message-actions {
    opacity: 1;
}

.message-actions .el-button {
    color: #64748b;
    font-size: 12px;
    padding: 4px 8px;
}

.message-actions .el-button:hover {
    color: #dc2626;
    background: rgba(220, 38, 38, 0.05);
}

/* 加载中 */
.loading-message {
    text-align: center;
    padding: 20px;
}

.loading-container {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.loading-animation {
    padding: 16px 24px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.wave-dots {
    display: flex;
    gap: 6px;
}

.wave-dots span {
    width: 10px;
    height: 10px;
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    border-radius: 50%;
    animation: wave 1.5s ease-in-out infinite;
}

.wave-dots span:nth-child(1) {
    animation-delay: 0s;
}

.wave-dots span:nth-child(2) {
    animation-delay: 0.1s;
}

.wave-dots span:nth-child(3) {
    animation-delay: 0.2s;
}

.wave-dots span:nth-child(4) {
    animation-delay: 0.3s;
}

.wave-dots span:nth-child(5) {
    animation-delay: 0.4s;
}

.loading-text {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
}

/* ========== 输入区域 ========== */
.chat-input-container {
    padding: 16px 24px 24px 24px;
    background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
    border-top: 1px solid #e8f0f8;
}

/* 快捷问题卡片 */
.quick-questions {
    margin-bottom: 16px;
    overflow: hidden;
}

.questions-scroll {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding: 8px 0;
    scrollbar-width: thin;
    scrollbar-color: #dc2626 #f1f5f9;
}

.questions-scroll::-webkit-scrollbar {
    height: 6px;
}

.questions-scroll::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
}

.questions-scroll::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
    border-radius: 3px;
}

.question-chip {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    border: 1px solid #fecaca;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    white-space: nowrap;
}

.question-chip:hover {
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    border-color: #fca5a5;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.15);
}

.chip-emoji {
    font-size: 18px;
}

.chip-text {
    font-size: 14px;
    font-weight: 500;
    color: #1e293b;
}

.input-wrapper {
    max-width: 900px;
    margin: 0 auto;
}

.input-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.input-label {
    font-size: 14px;
    font-weight: 600;
    color: #1e293b;
}

.input-hint {
    font-size: 12px;
    color: #94a3b8;
}

.input-body {
    display: flex;
    gap: 12px;
    align-items: flex-end;
}

.message-input {
    flex: 1;
}

.message-input :deep(.el-textarea__inner) {
    padding: 16px;
    border-radius: 12px;
    border: 2px solid #e2e8f0;
    font-size: 15px;
    line-height: 1.6;
    transition: all 0.3s ease;
    resize: none;
}

.message-input.has-content :deep(.el-textarea__inner) {
    border-color: #dc2626;
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.message-input :deep(.el-textarea__inner):focus {
    border-color: #dc2626;
    box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
}

.input-actions {
    display: flex;
    gap: 12px;
}

.action-buttons {
    display: flex;
    gap: 12px;
}

.clear-btn {
    padding: 12px 20px;
    border-radius: 10px;
    font-weight: 600;
    transition: all 0.3s ease;
}

.clear-btn:hover:not(:disabled) {
    background: #fef2f2;
    color: #dc2626;
    transform: translateY(-2px);
}

.send-button {
    padding: 12px 24px;
    border-radius: 10px;
    font-weight: 600;
    font-size: 15px;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

.send-button:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(220, 38, 38, 0.4);
}

/* ========== 动画 ========== */
@keyframes float {

    0%,
    100% {
        transform: translateY(0);
    }

    50% {
        transform: translateY(-20px);
    }
}

@keyframes morph {

    0%,
    100% {
        border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
    }

    50% {
        border-radius: 70% 30% 30% 70% / 70% 70% 30% 30%;
    }
}

@keyframes rise {
    0% {
        bottom: -10px;
        opacity: 0;
    }

    50% {
        opacity: 1;
    }

    100% {
        bottom: 100%;
        opacity: 0;
    }
}

@keyframes floating {

    0%,
    100% {
        transform: translateY(0);
    }

    50% {
        transform: translateY(-8px);
    }
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
        transform: scale(1);
    }

    50% {
        opacity: 0.5;
        transform: scale(0.9);
    }
}

@keyframes ripple {
    0% {
        width: 100%;
        height: 100%;
        opacity: 1;
    }

    100% {
        width: 200%;
        height: 200%;
        opacity: 0;
    }
}

@keyframes glow {

    0%,
    100% {
        opacity: 0.5;
        transform: translate(-50%, -50%) scale(1);
    }

    50% {
        opacity: 1;
        transform: translate(-50%, -50%) scale(1.2);
    }
}

@keyframes blink {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.3;
    }
}

@keyframes pulse-glow {

    0%,
    100% {
        box-shadow: 0 8px 24px rgba(220, 38, 38, 0.2);
    }

    50% {
        box-shadow: 0 8px 32px rgba(220, 38, 38, 0.4);
    }
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateY(20px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes wave {

    0%,
    100% {
        transform: translateY(0);
    }

    50% {
        transform: translateY(-8px);
    }
}

/* ========== 响应式设计 ========== */
@media (max-width: 1200px) {
    .chat-sidebar {
        width: 280px;
    }

    .message-content {
        max-width: 70%;
    }
}

@media (max-width: 768px) {
    .chat-container {
        padding: 12px;
    }

    .chat-layout {
        height: calc(100vh - 24px);
    }

    .chat-sidebar {
        display: none;
    }

    .message-content {
        max-width: 80%;
    }

    .page-header {
        padding: 16px;
    }

    .chat-history {
        padding: 16px;
    }

    .chat-input-container {
        padding: 16px;
    }
}

/* ========== 批量管理样式 ========== */
.sidebar-header {
    display: flex;
    align-items: center;
    gap: 10px;
}

.header-actions {
    display: flex;
    gap: 8px;
    align-items: center;
}

.batch-mode-btn {
    padding: 6px 12px;
}

.batch-delete-btn {
    padding: 6px 12px;
}

.cancel-batch-btn {
    padding: 6px 12px;
}

.chat-item.selected {
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    border-left: 3px solid #dc2626;
}

.chat-item-checkbox {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    flex-shrink: 0;
}

/* 批量模式下隐藏删除按钮 */
.is-batch-mode .delete-btn {
    display: none;
}

</style>