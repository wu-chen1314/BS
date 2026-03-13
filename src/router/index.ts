import { createRouter, createWebHistory, type RouteRecordRaw } from "vue-router";
import { ROUTE_TITLES } from "@/constants/heritage";
import { getCurrentUser } from "@/utils/session";

const Layout = () => import("../views/Layout.vue");
const Login = () => import("../views/Login.vue");

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "login",
    component: Login,
    meta: { title: ROUTE_TITLES.login, public: true, guestOnly: true },
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
        meta: { title: ROUTE_TITLES.home },
      },
      {
        path: "curation",
        name: "curation",
        component: () => import("../views/Curation.vue"),
        meta: { title: ROUTE_TITLES.curation },
      },
      {
        path: "heritage-trail",
        name: "heritage-trail",
        component: () => import("../views/HeritageTrail.vue"),
        meta: { title: ROUTE_TITLES["heritage-trail"] },
      },
      {
        path: "learning-studio",
        name: "learning-studio",
        component: () => import("../views/LearningStudio.vue"),
        meta: { title: ROUTE_TITLES["learning-studio"] },
      },
      {
        path: "region-category",
        name: "region-category",
        component: () => import("../views/RegionCategory.vue"),
        meta: { title: ROUTE_TITLES["region-category"] },
      },
      {
        path: "hot-ranking",
        name: "hot-ranking",
        component: () => import("../views/HotRanking.vue"),
        meta: { title: ROUTE_TITLES["hot-ranking"] },
      },
      {
        path: "chat",
        name: "chat",
        component: () => import("../views/Chat.vue"),
        meta: { title: ROUTE_TITLES.chat },
      },
      {
        path: "quiz",
        name: "quiz",
        component: () => import("../views/Quiz.vue"),
        meta: { title: "知识竞答" },
      },
      {
        path: "news",
        name: "news",
        component: () => import("../views/News.vue"),
        meta: { title: "资讯动态" },
      },
      {
        path: "favorites",
        name: "favorites",
        component: () => import("../views/Favorites.vue"),
        meta: { title: ROUTE_TITLES.favorites },
      },
      {
        path: "inheritor",
        name: "inheritor",
        component: () => import("../views/Inheritor.vue"),
        meta: { title: ROUTE_TITLES.inheritor, requireAdmin: true },
      },
      {
        path: "user",
        name: "user",
        component: () => import("../views/User.vue"),
        meta: { title: ROUTE_TITLES.user, requireAdmin: true },
      },
      {
        path: "profile",
        name: "profile",
        component: () => import("../views/Profile.vue"),
        meta: { title: ROUTE_TITLES.profile },
      },
      {
        path: "person",
        redirect: "/profile",
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, _from, next) => {
  const user = getCurrentUser();
  const isPublicPage = Boolean(to.meta.public);

  if (!isPublicPage && !user) {
    next({
      name: "login",
      query: {
        redirect: to.fullPath,
      },
    });
    return;
  }

  if (to.meta.guestOnly && user) {
    next({ path: "/home" });
    return;
  }

  if (to.meta.requireAdmin && user?.role !== "admin") {
    next({ path: "/home" });
    return;
  }

  if (typeof document !== "undefined") {
    const title = String(to.meta.title || "非遗数字传承平台");
    document.title = `${title} - 非遗数字传承平台`;
  }

  next();
});

export default router;
