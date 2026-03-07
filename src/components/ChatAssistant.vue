<template>
  <div class="chat-container" v-if="showAssistant">
    <el-button class="chat-fab" type="primary" circle size="large" @click="toggleChat">
      <el-icon size="24"><Service /></el-icon>
    </el-button>

    <transition name="el-zoom-in-bottom">
      <el-card v-show="isOpen" class="chat-window" shadow="always">
        <template #header>
          <div class="chat-header">
            <span class="chat-title">
              <el-icon><ChatDotRound /></el-icon>
              非遗智能助手
            </span>
            <div class="header-actions">
              <el-tooltip content="清空记录">
                <el-icon class="action-icon" @click="clearHistory"><Delete /></el-icon>
              </el-tooltip>
              <el-icon class="action-icon" @click="isOpen = false"><Close /></el-icon>
            </div>
          </div>
        </template>

        <div class="message-list" ref="messageListRef">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
          >
            <el-avatar :size="30" :src="msg.role === 'user' ? getUserAvatar : aiAvatar" class="msg-avatar" />

            <div class="msg-bubble">
              <div v-if="msg.role === 'ai'" class="ai-name">非遗小助手</div>
              <div class="msg-content" v-html="formatContent(msg.content)"></div>
            </div>
          </div>

          <div v-if="isLoading" class="message-item message-ai">
            <el-avatar :size="30" :src="aiAvatar" class="msg-avatar" />
            <div class="msg-bubble">
              <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="2"
            placeholder="请输入你的问题，Enter 发送，Shift + Enter 换行"
            resize="none"
            @keydown.enter.prevent="handleEnter"
          />
          <el-button type="primary" size="small" class="send-btn" @click="sendMessage" :loading="isLoading">
            发送
          </el-button>
        </div>
      </el-card>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { useRoute } from "vue-router";
import { ChatDotRound, Close, Delete, Service } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { buildStaticUrl } from "@/utils/url";

const route = useRoute();
const isOpen = ref(false);
const inputContent = ref("");
const isLoading = ref(false);
const messageListRef = ref<HTMLElement | null>(null);
const aiAvatar = "https://cdn-icons-png.flaticon.com/512/4712/4712027.png";

const showAssistant = computed(() => route.path !== "/login");

const userInfo = computed(() => {
  const userStr = sessionStorage.getItem("user");
  return userStr ? JSON.parse(userStr) : {};
});

const getUserAvatar = computed(() => {
  return (
    buildStaticUrl(userInfo.value.avatarUrl) ||
    "https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
  );
});

interface ChatMessage {
  role: "user" | "ai";
  content: string;
}

const welcomeText = "你好，我是非遗智能助手。你可以问我项目背景、历史故事、代表技艺，或让我要点式推荐适合浏览的非遗内容。";
const messages = ref<ChatMessage[]>([{ role: "ai", content: welcomeText }]);

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    scrollToBottom();
  }
};

const clearHistory = () => {
  messages.value = [{ role: "ai", content: "聊天记录已清空。你想继续了解哪一项非遗内容？" }];
};

const handleEnter = (event: KeyboardEvent) => {
  if (!event.shiftKey) {
    sendMessage();
  }
};

const sendMessage = async () => {
  const text = inputContent.value.trim();
  if (!text) return;

  messages.value.push({ role: "user", content: text });
  inputContent.value = "";
  scrollToBottom();
  isLoading.value = true;

  try {
    const res = await request.post("/chat/send", {
      message: text,
    });

    let aiReply = "";
    if (res.data.code === 200 && res.data.data) {
      aiReply = res.data.data.reply || JSON.stringify(res.data.data);
    } else {
      aiReply = res.data.msg || "抱歉，我暂时无法回答这个问题。";
    }

    messages.value.push({ role: "ai", content: aiReply });
  } catch (error) {
    console.error("AI assistant error", error);
    messages.value.push({ role: "ai", content: "抱歉，当前网络或模型服务暂不可用，请稍后重试。" });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

const formatContent = (text: string) => text.replace(/\n/g, "<br/>");
</script>

<style scoped>
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

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  color: #333;
}

.chat-title {
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
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
  color: #409eff;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 15px;
  height: 380px;
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

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-user .msg-bubble {
  background-color: #409eff;
  color: #fff;
  border-top-right-radius: 0;
}

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

.typing-indicator span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: #ccc;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
  margin: 0 2px;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

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

.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}
</style>
