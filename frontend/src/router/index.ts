import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/home/HomeView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import RegisterView from '@/views/auth/RegisterView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: 'Our Galaxy · 我们的宇宙', public: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { title: '登录 · Our Galaxy', public: true, guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { title: '注册 · Our Galaxy', public: true, guestOnly: true },
    },
    {
      path: '/memories',
      name: 'memories',
      component: () => import('@/views/memories/MemoriesView.vue'),
      meta: { title: '共同回忆 · Our Galaxy' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (to.meta.guestOnly && auth.token) {
      await auth.bootstrap()
      if (auth.isAuthenticated) return { name: 'memories' }
    }
    return true
  }

  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  await auth.bootstrap()
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = String(to.meta.title || 'Our Galaxy')
})

export default router
