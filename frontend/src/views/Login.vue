<template>
  <div class="auth-wrap">
    <div class="auth-card glass hover-up">
      <div class="brand">
        <div class="brand-logo">ERP</div>
        <div>
          <div class="brand-title">简易ERP <span class="ver">v1.0.0</span></div>
          <div class="brand-sub">订单 · 欠款管理</div>
        </div>
      </div>

      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="username" placeholder="登录名" size="large" autocomplete="username">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password
            autocomplete="current-password" @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="foot">
        <span class="text-mute">还没有账号？</span>
        <el-button link type="primary" @click="$router.push('/register')">注册新账号</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { me } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入登录名与密码')
    return
  }
  loading.value = true
  try {
    await store.login(username.value.trim(), password.value)
    // 拉取完整资料（含姓名）后再进入系统
    try {
      const u = await me()
      store.setProfile(u)
    } catch {
      /* 资料拉取失败不阻塞进入 */
    }
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1200px 600px at 15% -10%, rgba(96, 165, 250, 0.25), transparent 60%),
    radial-gradient(900px 500px at 110% 110%, rgba(37, 99, 235, 0.18), transparent 55%),
    var(--c-bg);
  padding: 20px;
}
.auth-card {
  width: 400px;
  padding: 34px 34px 26px;
  border-radius: 22px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 26px;
}
.brand-logo {
  width: 46px;
  height: 46px;
  border-radius: 13px;
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
  font-size: 20px;
  font-weight: 600;
}
.brand-sub {
  font-size: 12px;
  color: var(--c-text-mute);
}
.ver {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-primary);
  background: rgba(37, 99, 235, 0.1);
  border-radius: 6px;
  padding: 2px 6px;
  vertical-align: 2px;
}
.submit {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 6px;
}
.foot {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}
</style>
