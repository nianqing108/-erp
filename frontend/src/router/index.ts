import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/request'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true, title: '登录' } },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { public: true, title: '注册' } },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '仪表盘' } },
        { path: 'customers', name: 'customers', component: () => import('@/views/Customer.vue'), meta: { title: '客户管理' } },
        { path: 'orders', name: 'orders', component: () => import('@/views/OrderList.vue'), meta: { title: '订单管理' } },
        { path: 'statement', name: 'statement', component: () => import('@/views/Statement.vue'), meta: { title: '客户对账单' } },
        { path: 'monthly', name: 'monthly', component: () => import('@/views/MonthlyReport.vue'), meta: { title: '月度报表' } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  // 未登录访问业务页 → 登录页（携带回跳地址）；已登录访问登录页 → 仪表盘
  if (!to.meta.public && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && getToken()) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
