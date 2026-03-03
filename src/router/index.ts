import { createRouter, createWebHistory } from "vue-router";
import Layout from "../views/Layout.vue"; // 使用相对路径 ..
import Login from "../views/Login.vue"; // 使用相对路径 ..

const router = createRouter({
  history: createWebHistory(),
  //先跳到login页面，再跳到home页面

  routes: [
    {
      path: "/login",
      name: "login",
      component: Login,
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
        },
        {
          path: "inheritor",
          name: "inheritor",
          component: () => import("../views/Inheritor.vue"),
        },
        {
          path: "user",
          name: "user",
          component: () => import("../views/User.vue"),
        },
        {
          path: "profile",
          name: "profile",
          component: () => import("../views/Profile.vue"),
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
          meta: { title: "AI 聊天" },
        },
        {
          path: "person",
          name: "Person",
          component: () => import("../views/Profile.vue"),
          meta: { title: "个人中心" },
        },
        // ===== 新增：地区分类路由 =====
        {
          path: "region-category",
          name: "region-category",
          component: () => import("../views/RegionCategory.vue"),
          meta: { title: "地区分类" },
        },
        // ===========================
      ],
    },
  ],
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const user = sessionStorage.getItem("user");
  // 检查是否是登录页面或公开页面
  const publicPages = ["/login"];
  const isPublicPage = publicPages.includes(to.path);

  if (!isPublicPage && !user) {
    next({ name: "login" });
  } else {
    next();
  }
});

export default router;
