<template>
  <div class="auth-wrap">
    <div class="auth-card glass hover-up">
      <div class="brand">
        <div class="brand-logo">ERP</div>
        <div>
          <div class="brand-title">注册账号</div>
          <div class="brand-sub">开通后即可登录使用</div>
        </div>
      </div>

      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="登录名（3-32 位字母/数字/下划线）" size="large"
            autocomplete="username">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.realName" placeholder="姓名（选填）" size="large">
            <template #prefix><el-icon><Postcard /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码（6-32 位）" size="large"
            show-password autocomplete="new-password">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" size="large" show-password
            @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.inviteCode" placeholder="注册邀请码（如单位未启用可留空）" size="large">
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
          注 册
        </el-button>
      </el-form>

      <div class="foot">
        <span class="text-mute">已有账号？</span>
        <el-button link type="primary" @click="$router.push('/login')">直接登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Postcard } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', realName: '', password: '', confirm: '', inviteCode: '' })
const loading = ref(false)

async function submit() {
  const name = form.username.trim()
  if (!/^[a-zA-Z0-9_]{3,32}$/.test(name)) {
    ElMessage.warning('登录名须为 3-32 位字母、数字或下划线')
    return
  }
  if (form.password.length < 6 || form.password.length > 32) {
    ElMessage.warning('密码长度须为 6-32 位')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await store.register({
      username: name,
      password: form.password,
      realName: form.realName.trim() || undefined,
      inviteCode: form.inviteCode.trim() || undefined
    })
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
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
  margin-bottom: 22px;
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
.submit {
  width: 100%;
  margin-top: 4px;
  letter-spacing: 6px;
}
.foot {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}
</style>
