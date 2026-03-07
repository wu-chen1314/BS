import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '../views/Layout.vue'
import Login from '../views/Login.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
    },
    {
      path: '/',
      component: Layout,
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('../views/Home.vue'),
        },
        {
          path: 'inheritor',
          name: 'inheritor',
          component: () => import('../views/Inheritor.vue'),
          meta: { title: '传承人管理', requireAdmin: true },
        },
        {
          path: 'user',
          name: 'user',
          component: () => import('../views/User.vue'),
          meta: { title: '用户管理', requireAdmin: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/Profile.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'favorites',
          name: 'favorites',
          component: () => import('../views/Favorites.vue'),
          meta: { title: '我的收藏' },
        },
        {
          path: 'hot-ranking',
          name: 'hot-ranking',
          component: () => import('../views/HotRanking.vue'),
          meta: { title: '热门排行' },
        },
        {
          path: 'chat',
          name: 'chat',
          component: () => import('../views/Chat.vue'),
          meta: { title: 'AI 聊天' },
        },
        {
          path: 'person',
          name: 'Person',
          component: () => import('../views/Profile.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'audit-center',
          name: 'audit-center',
          component: () => import('../views/AuditCenter.vue'),
          meta: { title: '非遗审核台', requireAdmin: true },
        },
        {
          path: 'region-category',
          name: 'region-category',
          component: () => import('../views/RegionCategory.vue'),
          meta: { title: '地区分类' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const userStr = sessionStorage.getItem('user')
  const isPublicPage = to.path === '/login'
  const user = userStr ? JSON.parse(userStr) : null

  if (!isPublicPage && !user) {
    next({ name: 'login' })
    return
  }

  if (to.meta.requireAdmin && user?.role !== 'admin') {
    ElMessage.error('仅管理员可访问该页面')
    next({ name: 'home' })
    return
  }

  next()
})

export default router