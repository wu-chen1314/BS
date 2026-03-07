<script setup lang="ts">
import { defineAsyncComponent } from "vue";
import { RouterView } from "vue-router";

const ChatAssistant = defineAsyncComponent(() => import("./components/ChatAssistant.vue"));
</script>

<template>
  <RouterView />
  <ChatAssistant />
</template>

<style>
html,
body,
#app {
  margin: 0;
  padding: 0;
  height: 100%;
}
</style>
