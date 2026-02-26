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
          component: () => import("../views/Home.vue"), // ✨ 修复点 2：使用相对路径
        },
        {
          path: "inheritor",
          name: "inheritor",
          // ✨ 修复点 3：确保文件名大小写正确 (I 大写)
          component: () => import("../views/Inheritor.vue"),
        },
        {
          path: "user",
          name: "user",
          // ✨ 修复点 4：新增的用户管理页面
          component: () => import("../views/User.vue"),
        },
        {
          path: "profile",
          name: "profile",
          component: () => import("../views/Profile.vue"),
        },
        {
          path: "person",
          name: "Person",
          component: () => import("../views/person.vue"),
          meta: { title: "个人中心" },
        },
      ],
    },
  ],
});

// 路由守卫
router.beforeEach((to, from, next) => {
  const user = localStorage.getItem("user");
  if (to.name !== "login" && !user) {
    next({ name: "login" });
  } else {
    next();
  }
});

export default router;
