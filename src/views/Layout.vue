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

      <el-menu
        class="sidebar-menu"
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="#f7efe4"
        active-text-color="#ffe5b8"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.index" :index="item.index">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>

      <div v-if="!isCollapse" class="sidebar-foot">
        <span class="foot-label">当前身份</span>
        <strong>{{ userInfo.role === "admin" ? "平台管理员" : "文化探索者" }}</strong>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div class="topbar-left">
          <button type="button" class="collapse-trigger" @click="isCollapse = !isCollapse">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
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

          <el-popover placement="bottom" :width="280" trigger="click">
            <template #reference>
              <button type="button" class="notice-trigger">
                <el-icon><Bell /></el-icon>
              </button>
            </template>
            <div class="notice-panel">
              <strong>平台提醒</strong>
              <p>近期重点关注地区分类、主题策展和非遗路线的联动展示。</p>
            </div>
          </el-popover>

          <el-dropdown @command="handleCommand">
            <button type="button" class="user-entry">
              <el-avatar :size="34" :src="avatarUrl" />
              <div class="user-copy">
                <strong>{{ userInfo.nickname || userInfo.username || "未登录用户" }}</strong>
                <span>{{ userInfo.role === "admin" ? "管理员" : "普通用户" }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
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
import { computed, onMounted, ref } from "vue";
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
  Setting,
  Star,
  TrendCharts,
  User,
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { buildStaticUrl } from "@/utils/url";

const router = useRouter();
const route = useRoute();
const isCollapse = ref(false);

const userInfo = ref<any>({
  role: "user",
  nickname: "",
  username: "",
  avatarUrl: "",
});

const menus = [
  { index: "/home", label: "非遗项目", icon: DataLine },
  { index: "/curation", label: "主题策展", icon: CollectionTag },
  { index: "/heritage-trail", label: "非遗路线", icon: Guide },
  { index: "/region-category", label: "地区分类", icon: Location },
  { index: "/hot-ranking", label: "热度排行", icon: TrendCharts },
  { index: "/chat", label: "AI 助手", icon: ChatLineRound },
  { index: "/favorites", label: "我的收藏", icon: Star, roles: ["user"] },
  { index: "/inheritor", label: "传承人管理", icon: User, roles: ["admin"] },
  { index: "/user", label: "用户管理", icon: Setting, roles: ["admin"] },
];

const visibleMenus = computed(() =>
  menus.filter((item) => !item.roles || item.roles.includes(userInfo.value.role || "user"))
);

const activeMenu = computed(() => route.path);

const pageTitle = computed(() => String(route.meta.title || "非遗数字传承平台"));

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title && item.path !== "/")
    .map((item) => ({
      path: item.path,
      title: String(item.meta.title),
    }))
);

const avatarUrl = computed(
  () =>
    buildStaticUrl(userInfo.value.avatarUrl) ||
    "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"
);

const handleMenuSelect = (index: string) => {
  router.push(index);
};

const handleCommand = (command: string) => {
  if (command === "profile") {
    router.push("/profile");
    return;
  }

  if (command === "logout") {
    ElMessageBox.confirm("确认退出当前账号吗？", "提示", { type: "warning" })
      .then(() => {
        sessionStorage.removeItem("user");
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("tokenExpires");
        router.push("/login");
        ElMessage.success("已退出登录");
      })
      .catch(() => {});
  }
};

const loadUserInfo = () => {
  const userStr = sessionStorage.getItem("user");
  if (!userStr) {
    return;
  }

  try {
    userInfo.value = JSON.parse(userStr);
  } catch (error) {
    console.error("Failed to parse user session", error);
  }
};

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: auto 1fr;
  background:
    radial-gradient(circle at top left, rgba(196, 30, 58, 0.12), transparent 28%),
    linear-gradient(180deg, #f7f3ee 0%, #eef3f7 100%);
}

.sidebar {
  width: 272px;
  background:
    linear-gradient(180deg, rgba(32, 44, 63, 0.98) 0%, rgba(53, 71, 96, 0.98) 100%),
    #233044;
  color: #f7efe4;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: width 0.24s ease;
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
  color: #fff6e4;
  background: linear-gradient(135deg, #c41e3a, #d8863d);
  box-shadow: 0 12px 28px rgba(196, 30, 58, 0.28);
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
  background: linear-gradient(90deg, rgba(196, 30, 58, 0.92), rgba(216, 134, 61, 0.82));
  box-shadow: 0 10px 24px rgba(196, 30, 58, 0.24);
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
  border: none;
  background: #fff;
  cursor: pointer;
}

.collapse-trigger {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 18px;
  box-shadow: 0 10px 28px rgba(44, 62, 80, 0.08);
}

.page-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-meta h1 {
  margin: 0;
  font-size: 26px;
  color: #1f2c3d;
}

.page-kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.24em;
  color: #a66a3f;
}

.notice-trigger {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  box-shadow: 0 10px 28px rgba(44, 62, 80, 0.08);
}

.notice-panel strong {
  display: block;
  margin-bottom: 8px;
  color: #1f2c3d;
}

.notice-panel p {
  margin: 0;
  line-height: 1.7;
  color: #5f6f82;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 16px;
  padding: 8px 12px 8px 8px;
  box-shadow: 0 10px 28px rgba(44, 62, 80, 0.08);
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.user-copy strong {
  font-size: 14px;
  color: #1f2c3d;
}

.user-copy span {
  font-size: 12px;
  color: #8a96a3;
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
