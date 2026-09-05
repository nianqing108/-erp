import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, register as apiRegister } from '@/api/auth'
import type { UserVO } from '@/types'

const TOKEN_KEY = 'erp_token'
const USER_KEY = 'erp_user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<UserVO | null>(safeParse(localStorage.getItem(USER_KEY)))

  function safeParse(raw: string | null): UserVO | null {
    if (!raw) return null
    try {
      return JSON.parse(raw) as UserVO
    } catch {
      return null
    }
  }

  function persist(t: string, u: UserVO | null) {
    token.value = t
    user.value = u
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u))
    else localStorage.removeItem(USER_KEY)
  }

  async function login(username: string, password: string) {
    const { token: t } = await apiLogin({ username, password })
    persist(t, { id: 0, username })
    // 登录后拉取完整用户信息（昵称等）
    return t
  }

  async function register(payload: { username: string; password: string; realName?: string; inviteCode?: string }) {
    const u = await apiRegister(payload)
    return u
  }

  function setProfile(u: UserVO) {
    persist(token.value, u)
  }

  function logout() {
    persist('', null)
  }

  return { token, user, login, register, setProfile, logout }
})
