<template>
  <div class="layout-shell">
    <aside class="sidebar" :class="{ collapsed: isCollapse }">
      <div class="brand">
        <div class="brand-mark">遗</div>
        <div v-if="!isCollapse" class="brand-copy">
          <strong>非遗数字传承平台</strong>
          <span>Intangible Heritage Hub</span>
        </div>
      </div>

      <el-menu class="sidebar-menu" :default-active="activeMenu" :collapse="isCollapse" background-color="transparent"
        text-color="#f7efe4" active-text-color="#ffe5b8" @select="handleMenuSelect">
        <el-menu-item v-for="item in visibleMenus" :key="item.index" :index="item.index">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div v-if="!isCollapse" class="sidebar-foot">
        <span class="foot-label">当前身份</span>
        <strong>{{ userRoleLabel }}</strong>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div class="topbar-left">
          <button type="button" class="collapse-trigger" @click="isCollapse = !isCollapse">
            <el-icon>
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </button>
          <div class="page-meta">
            <p class="page-kicker">DIGITAL HERITAGE EXPERIENCE</p>
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="topbar-right">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>

          <el-popover placement="bottom" :width="300" trigger="click">
            <template #reference>
              <button type="button" class="notice-trigger">
                <el-icon>
                  <Bell />
                </el-icon>
              </button>
            </template>
            <div class="notice-panel">
              <strong>平台提醒</strong>
              <p>近期重点优化主题策展、非遗路线、地区分类和研学工坊的联动体验。</p>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <button type="button" class="user-entry">
              <el-avatar :size="34" :src="avatarUrl" />
              <div class="user-copy">
                <strong>{{ userInfo.nickname || userInfo.username || "未登录用户" }}</strong>
                <span>{{ userRoleLabel }}</span>
              </div>
              <el-icon>
                <ArrowDown />
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content-panel">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, type Component } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowDown,
  Bell,
  ChatLineRound,
  CollectionTag,
  DataLine,
  Expand,
  Fold,
  Guide,
  Location,
  Reading,
  Setting,
  Star,
  TrendCharts,
  User,
  Trophy,
  Document,
} from "@element-plus/icons-vue";
import {
  getCurrentUser,
  SESSION_CHANGED_EVENT,
  type SessionUser,
} from "@/utils/session";
import { MATERIAL_PLACEHOLDERS } from "@/constants/materials";
import { buildStaticUrl } from "@/utils/url";
import { useHeritageHubStore } from "@/stores/heritageHub";
import errorHandler from "@/utils/errorHandler";
import { confirmManualLogout, isDialogDismissError, performLogout } from "@/utils/logout";

type UserRole = "admin" | "user";

interface MenuItem {
  index: string;
  label: string;
  icon: Component;
  roles?: UserRole[];
}

const DEFAULT_USER: SessionUser = {
  role: "user",
  nickname: "",
  username: "",
  avatarUrl: "",
};

const menus: MenuItem[] = [
  { index: "/home", label: "非遗项目", icon: DataLine },
  { index: "/news", label: "资讯动态", icon: Document },
  { index: "/quiz", label: "知识竞答", icon: Trophy },
  { index: "/curation", label: "主题策展", icon: CollectionTag },
  { index: "/heritage-trail", label: "非遗路线", icon: Guide },
  { index: "/learning-studio", label: "研学工坊", icon: Reading },
  { index: "/region-category", label: "地区分类", icon: Location },
  { index: "/hot-ranking", label: "热度排行", icon: TrendCharts },
  { index: "/chat", label: "AI 助手", icon: ChatLineRound },
  { index: "/favorites", label: "我的收藏", icon: Star, roles: ["user"] },
  { index: "/inheritor", label: "传承人管理", icon: User, roles: ["admin"] },
  { index: "/user", label: "用户管理", icon: Setting, roles: ["admin"] },
];

const router = useRouter();
const route = useRoute();
const hubStore = useHeritageHubStore();
const isCollapse = ref(false);
const userInfo = ref<SessionUser>({ ...DEFAULT_USER });
const hasSyncedSession = ref(false);
const hadAuthenticatedUser = ref(false);
const logoutPending = ref(false);

const normalizedUserRole = computed<UserRole>(() => (userInfo.value.role === "admin" ? "admin" : "user"));

const visibleMenus = computed(() =>
  menus.filter((item) => !item.roles || item.roles.includes(normalizedUserRole.value))
);

const activeMenu = computed(() => {
  const matchedMenu = [...visibleMenus.value]
    .sort((left, right) => right.index.length - left.index.length)
    .find((item) => route.path === item.index || route.path.startsWith(`${item.index}/`));

  return matchedMenu?.index || "";
});

const pageTitle = computed(() => String(route.meta.title || "非遗数字传承平台"));

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title && item.path !== "/" && item.path !== "/home")
    .map((item) => ({
      path: item.path,
      title: String(item.meta.title),
    }))
);

const avatarUrl = computed(
  () =>
    buildStaticUrl(userInfo.value.avatarUrl) || MATERIAL_PLACEHOLDERS.avatar
);

const userRoleLabel = computed(() => (normalizedUserRole.value === "admin" ? "平台管理员" : "文化探索者"));

const handleMenuSelect = (index: string) => {
  if (route.path === index) {
    return;
  }

  router.push(index);
};

const handleCommand = async (command: string) => {
  if (command === "profile") {
    await router.push("/profile");
    return;
  }

  if (command === "logout") {
    if (logoutPending.value) {
      return;
    }

    try {
      logoutPending.value = true;
      await confirmManualLogout({
        user: userInfo.value,
      });
      await performLogout({
        clearState: () => hubStore.clearAllCaches(),
        hardRedirect: true,
        router,
        showSuccessMessage: true,
      });
    } catch (error: any) {
      if (isDialogDismissError(error)) {
        return;
      }

      console.error("退出登录失败:", error);
      errorHandler.showSimpleError("退出登录失败，请稍后重试");
    } finally {
      logoutPending.value = false;
    }
  }
};

const loadUserInfo = () => {
  const user = getCurrentUser();
  const shouldClearCaches = !user && (!hasSyncedSession.value || hadAuthenticatedUser.value);
  userInfo.value = user || { ...DEFAULT_USER };
  hasSyncedSession.value = true;
  hadAuthenticatedUser.value = Boolean(user);

  if (shouldClearCaches) {
    hubStore.clearAllCaches();
  }
};

watch(
  () => route.fullPath,
  () => {
    loadUserInfo();
  },
  { immediate: true }
);

onMounted(() => {
  window.addEventListener(SESSION_CHANGED_EVENT, loadUserInfo);
});

onBeforeUnmount(() => {
  window.removeEventListener(SESSION_CHANGED_EVENT, loadUserInfo);
});
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: auto 1fr;
  background:
    radial-gradient(circle at top left, rgba(192, 138, 63, 0.14), transparent 30%),
    radial-gradient(circle at 82% 8%, rgba(164, 59, 47, 0.08), transparent 18%),
    linear-gradient(180deg, var(--heritage-paper-soft) 0%, var(--heritage-paper) 100%);
}

.sidebar {
  width: 272px;
  background: rgba(28, 40, 51, 0.85);
  /* heritage-ink with transparency */
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  color: #f7efe4;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
}

.sidebar.collapsed {
  width: 88px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 8px 12px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--heritage-paper-soft);
  background: linear-gradient(135deg, var(--heritage-primary), var(--heritage-primary-soft));
  box-shadow: 0 12px 28px rgba(192, 57, 43, 0.3);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  font-size: 17px;
  letter-spacing: 0.02em;
}

.brand-copy span {
  font-size: 12px;
  color: rgba(247, 239, 228, 0.7);
}

.sidebar-menu {
  border: none;
  flex: 1;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 48px;
  border-radius: 14px;
  margin-bottom: 8px;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(192, 57, 43, 0.9), rgba(230, 126, 34, 0.8));
  box-shadow: 0 8px 20px rgba(192, 57, 43, 0.25);
  transform: translateX(4px);
  transition: all 0.3s ease;
}

.sidebar-foot {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 16px 10px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.foot-label {
  font-size: 12px;
  color: rgba(247, 239, 228, 0.65);
}

.main-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 24px 28px 18px;
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-trigger,
.notice-trigger,
.user-entry {
  border: 1px solid var(--heritage-glass-border);
  background: var(--heritage-glass-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  cursor: pointer;
  transition: all 0.3s ease;
}

.collapse-trigger:hover,
.notice-trigger:hover,
.user-entry:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: var(--heritage-card-shadow-hover);
}

.collapse-trigger {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 18px;
  box-shadow: var(--heritage-glass-shadow);
}

.page-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-meta h1 {
  margin: 0;
  font-size: 26px;
  color: var(--heritage-ink);
}

.page-kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.24em;
  color: var(--heritage-gold);
}

.notice-trigger {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  box-shadow: var(--heritage-glass-shadow);
}

.notice-panel strong {
  display: block;
  margin-bottom: 8px;
  color: var(--heritage-ink);
}

.notice-panel p {
  margin: 0;
  line-height: 1.7;
  color: var(--heritage-ink-soft);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 16px;
  padding: 8px 14px 8px 8px;
  box-shadow: var(--heritage-glass-shadow);
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.user-copy strong {
  font-size: 14px;
  color: var(--heritage-ink);
}

.user-copy span {
  font-size: 12px;
  color: var(--heritage-muted);
}

.content-panel {
  min-width: 0;
  padding: 0 24px 24px;
}

@media (max-width: 1100px) {
  .layout-shell {
    grid-template-columns: 88px 1fr;
  }

  .sidebar {
    width: 88px;
  }

  .sidebar:not(.collapsed) {
    width: 88px;
  }

  .brand-copy,
  .sidebar-foot {
    display: none;
  }
}

@media (max-width: 768px) {
  .layout-shell {
    display: block;
  }

  .sidebar {
    width: 100%;
    height: auto;
    padding-bottom: 8px;
  }

  .topbar {
    padding: 18px 16px 12px;
    flex-direction: column;
    align-items: stretch;
  }

  .topbar-left,
  .topbar-right {
    justify-content: space-between;
    width: 100%;
  }

  .content-panel {
    padding: 0 12px 18px;
  }
}
</style>
