import { createRouter, createWebHistory } from "vue-router";

const Layout = () => import("../views/Layout.vue");
const Login = () => import("../views/Login.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: Login,
      meta: { title: "登录" },
    },
    {
      path: "/",
      component: Layout,
      redirect: "/home",
      children: [
        {
          path: "home",
          name: "home",
          component: () => import("../views/Home.vue"),
          meta: { title: "非遗项目" },
        },
        {
          path: "curation",
          name: "curation",
          component: () => import("../views/Curation.vue"),
          meta: { title: "主题策展" },
        },
        {
          path: "heritage-trail",
          name: "heritage-trail",
          component: () => import("../views/HeritageTrail.vue"),
          meta: { title: "非遗路线" },
        },
        {
          path: "inheritor",
          name: "inheritor",
          component: () => import("../views/Inheritor.vue"),
          meta: { title: "传承人管理", requireAdmin: true },
        },
        {
          path: "user",
          name: "user",
          component: () => import("../views/User.vue"),
          meta: { title: "用户管理", requireAdmin: true },
        },
        {
          path: "profile",
          name: "profile",
          component: () => import("../views/Profile.vue"),
          meta: { title: "个人中心" },
        },
        {
          path: "favorites",
          name: "favorites",
          component: () => import("../views/Favorites.vue"),
          meta: { title: "我的收藏" },
        },
        {
          path: "hot-ranking",
          name: "hot-ranking",
          component: () => import("../views/HotRanking.vue"),
          meta: { title: "热度排行" },
        },
        {
          path: "chat",
          name: "chat",
          component: () => import("../views/Chat.vue"),
          meta: { title: "AI 助手" },
        },
        {
          path: "region-category",
          name: "region-category",
          component: () => import("../views/RegionCategory.vue"),
          meta: { title: "地区分类" },
        },
        {
          path: "person",
          name: "person",
          component: () => import("../views/Profile.vue"),
          meta: { title: "个人中心" },
        },
      ],
    },
  ],
});

router.beforeEach((to, from, next) => {
  const userStr = sessionStorage.getItem("user");
  const publicPages = ["/login"];
  const isPublicPage = publicPages.includes(to.path);

  if (!isPublicPage && !userStr) {
    next({ name: "login" });
    return;
  }

  if (to.meta.requireAdmin) {
    try {
      const user = userStr ? JSON.parse(userStr) : null;
      if (user?.role !== "admin") {
        next({ path: "/home" });
        return;
      }
    } catch (error) {
      console.error("Failed to parse current user", error);
      next({ name: "login" });
      return;
    }
  }

  next();
});

export default router;
