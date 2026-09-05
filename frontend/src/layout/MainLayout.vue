<template>
  <div class="layout">
    <aside class="layout-sider glass">
      <div class="brand">
        <div class="brand-logo">ERP</div>
        <div class="brand-text">
          <div class="brand-title">简易ERP</div>
          <div class="brand-sub">订单 · 欠款管理</div>
        </div>
      </div>
      <el-menu :default-active="active" router class="sider-menu" background-color="transparent">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="layout-main">
      <header class="layout-topbar glass">
        <div class="topbar-title">{{ currentTitle }}</div>
        <div class="topbar-right">
          <el-tag type="primary" effect="plain" round>v1.0.0</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-icon><UserFilled /></el-icon>
              {{ displayName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ store.user?.username }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  User,
  UserFilled,
  ArrowDown,
  Document,
  Tickets,
  DataLine
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const menus = [
  { path: '/dashboard', title: '仪表盘', icon: Odometer },
  { path: '/customers', title: '客户管理', icon: User },
  { path: '/orders', title: '订单管理', icon: Document },
  { path: '/statement', title: '客户对账单', icon: Tickets },
  { path: '/monthly', title: '月度报表', icon: DataLine }
]

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const active = computed(() => '/' + (route.path.split('/')[1] || 'dashboard'))
const currentTitle = computed(() => (route.meta.title as string) || '简易ERP')
const displayName = computed(() => store.user?.realName || store.user?.username || '未登录')

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    store.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
}
.layout-sider {
  width: 232px;
  margin: 14px 0 14px 14px;
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px 18px;
}
.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 1px;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
}
.brand-title {
  font-weight: 600;
  font-size: 16px;
}
.brand-sub {
  font-size: 12px;
  color: var(--c-text-mute);
}
.sider-menu {
  border-right: none;
  flex: 1;
}
.sider-menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin-bottom: 6px;
  height: 46px;
}
.sider-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(96, 165, 250, 0.1));
  color: var(--c-primary);
  font-weight: 600;
}
.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.layout-topbar {
  margin: 14px 14px 0;
  height: 58px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
}
.topbar-title {
  font-size: 17px;
  font-weight: 600;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 10px;
  background: var(--c-bg-tint);
  color: var(--c-primary);
  font-size: 13px;
  cursor: pointer;
  outline: none;
}
.layout-content {
  flex: 1;
  overflow: auto;
}
</style>
