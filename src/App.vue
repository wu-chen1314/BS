<script setup lang="ts">
import { RouterView } from 'vue-router'
// 1. 引入刚才写好的聊天组件
import ChatAssistant from './components/ChatAssistant.vue'
</script>

<template>
  <RouterView />

  <ChatAssistant />
</template>

<style>
/* 这里可以放一些全局样式，如果没有原本的内容可以留空 */
html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>