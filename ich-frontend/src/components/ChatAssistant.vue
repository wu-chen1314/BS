<template>
    <div class="chat-container">
        <el-button class="chat-fab" type="primary" circle size="large" @click="toggleChat">
            <el-icon size="24">
                <Service />
            </el-icon>
        </el-button>

        <transition name="el-zoom-in-bottom">
            <el-card v-show="isOpen" class="chat-window" shadow="always">
                <template #header>
                    <div class="chat-header">
                        <span style="font-weight: bold; display: flex; align-items: center; gap: 5px;">
                            <el-icon>
                                <ChatDotRound />
                            </el-icon> 非遗智能助手
                        </span>
                        <div class="header-actions">
                            <el-tooltip content="清空记录">
                                <el-icon class="action-icon" @click="clearHistory">
                                    <Delete />
                                </el-icon>
                            </el-tooltip>
                            <el-icon class="action-icon" @click="isOpen = false">
                                <Close />
                            </el-icon>
                        </div>
                    </div>
                </template>

                <div class="message-list" ref="messageListRef">
                    <div v-for="(msg, index) in messages" :key="index" class="message-item"
                        :class="msg.role === 'user' ? 'message-user' : 'message-ai'">
                        <el-avatar :size="30"
                            :src="msg.role === 'user' ? (userInfo.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png') : aiAvatar"
                            class="msg-avatar" />

                        <div class="msg-bubble">
                            <div v-if="msg.role === 'ai'" class="ai-name">非遗小助手</div>
                            <div class="msg-content" v-html="formatContent(msg.content)"></div>
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
                    <el-input v-model="inputContent" type="textarea" :rows="2" placeholder="请输入您的问题... (Enter 发送)"
                        resize="none" @keydown.enter.prevent="handleEnter" />
                    <el-button type="primary" size="small" class="send-btn" @click="sendMessage" :loading="isLoading">
                        发送
                    </el-button>
                </div>
            </el-card>
        </transition>
    </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { Service, ChatDotRound, Close, Delete } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// --- 状态定义 ---
const isOpen = ref(false)
const inputContent = ref('')
const isLoading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const aiAvatar = 'https://cdn-icons-png.flaticon.com/512/4712/4712027.png' // AI 头像

// 从 localStorage 获取当前用户信息
const userInfo = computed(() => {
    const userStr = localStorage.getItem('user')
    return userStr ? JSON.parse(userStr) : {}
})

// 消息列表结构：role = 'user' | 'ai'
interface ChatMessage {
    role: 'user' | 'ai'
    content: string
}

const messages = ref<ChatMessage[]>([
    { role: 'ai', content: '您好！我是您的非遗智能助手。我可以为您介绍非遗项目、查询历史渊源，或者推荐有趣的非遗文化。' }
])

// --- 核心方法 ---

// 1. 切换开关
const toggleChat = () => {
    isOpen.value = !isOpen.value
    if (isOpen.value) scrollToBottom()
}

// 2. 清空记录
const clearHistory = () => {
    messages.value = [{ role: 'ai', content: '记录已清空，有什么可以帮您的吗？' }]
}

// 3. 处理回车 (Enter 发送, Shift+Enter 换行)
const handleEnter = (e: KeyboardEvent) => {
    if (!e.shiftKey) {
        sendMessage()
    }
}

// 4. 发送消息逻辑
const sendMessage = async () => {
    const text = inputContent.value.trim()
    if (!text) return

    // 1. 添加用户消息
    messages.value.push({ role: 'user', content: text })
    inputContent.value = '' // 清空输入框
    scrollToBottom()
    isLoading.value = true

    try {
        // ✨✨ 真实对接后端时，请解开这段注释 ✨✨
        
        const res = await axios.post('http://localhost:8080/api/chat/send', {
          message: text,
          userId: userInfo.value.id
        })
        const aiReply = res.data.data
        

      

        // 2. 添加 AI 回复
        messages.value.push({ role: 'ai', content: aiReply })

    } catch (error) {
        messages.value.push({ role: 'ai', content: '抱歉，我好像断网了，请稍后再试。' })
    } finally {
        isLoading.value = false
        scrollToBottom()
    }
}

// 5. 自动滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (messageListRef.value) {
            messageListRef.value.scrollTop = messageListRef.value.scrollHeight
        }
    })
}

// 6. 简单的换行符处理
const formatContent = (text: string) => {
    return text.replace(/\n/g, '<br/>')
}

// --- 模拟 AI 智能回复逻辑 (Mock) ---
const mockAiResponse = (question: string): string => {
    if (question.includes('你好') || question.includes('Hi')) return '您好！很高兴为您服务。'
    if (question.includes('非遗') || question.includes('项目')) return '你可以点击顶部的“新增项目”来录入新的非遗文化，或者在搜索栏查询现有的项目。'
    if (question.includes('管理员') || question.includes('权限')) return '管理员用户拥有审核、删除和编辑所有项目的最高权限。'
    if (question.includes('视频') || question.includes('上传')) return '上传视频时，请确保文件大小不超过 100MB，并且格式为 MP4。'
    return `您刚才说的是：“${question}” 吗？\n这是个很有趣的话题！作为一个演示助手，我目前只能回答特定问题，但您可以联系开发者接入 ChatGPT 或 DeepSeek API 来让我变得更聪明！`
}
</script>

<style scoped>
/* 悬浮按钮位置 */
.chat-fab {
    position: fixed;
    bottom: 30px;
    right: 30px;
    z-index: 2000;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    transition: transform 0.3s;
}

.chat-fab:hover {
    transform: scale(1.1) rotate(15deg);
}

/* 聊天窗口主体 */
.chat-window {
    position: fixed;
    bottom: 90px;
    right: 30px;
    width: 380px;
    height: 550px;
    z-index: 2001;
    display: flex;
    flex-direction: column;
    border-radius: 12px;
}

/* 头部样式 */
.chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    color: #333;
}

.header-actions {
    display: flex;
    gap: 10px;
}

.action-icon {
    cursor: pointer;
    color: #999;
    transition: color 0.3s;
}

.action-icon:hover {
    color: #409EFF;
}

/* 消息列表区 (可滚动) */
.message-list {
    flex: 1;
    overflow-y: auto;
    padding: 15px;
    background-color: #f5f7fa;
    display: flex;
    flex-direction: column;
    gap: 15px;
    height: 380px;
    /* 固定高度确保滚动 */
}

.message-item {
    display: flex;
    gap: 10px;
    max-width: 85%;
}

.msg-avatar {
    flex-shrink: 0;
}

.msg-bubble {
    padding: 10px 14px;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.5;
    word-break: break-all;
    position: relative;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

/* AI 样式 (左侧) */
.message-ai {
    align-self: flex-start;
}

.message-ai .msg-bubble {
    background-color: #fff;
    color: #333;
    border-top-left-radius: 0;
}

.ai-name {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
}

/* 用户 样式 (右侧) */
.message-user {
    align-self: flex-end;
    flex-direction: row-reverse;
}

.message-user .msg-bubble {
    background-color: #409EFF;
    color: #fff;
    border-top-right-radius: 0;
}

/* 底部输入区 */
.chat-input-area {
    padding: 10px;
    border-top: 1px solid #ebeef5;
    background: #fff;
    position: relative;
}

.send-btn {
    position: absolute;
    bottom: 15px;
    right: 15px;
}

/* 打字机动画点 */
.typing-indicator span {
    display: inline-block;
    width: 6px;
    height: 6px;
    background-color: #ccc;
    border-radius: 50%;
    animation: typing 1.4s infinite ease-in-out both;
    margin: 0 2px;
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

/* 滚动条美化 */
.message-list::-webkit-scrollbar {
    width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
}
</style>