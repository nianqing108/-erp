import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const TOKEN_KEY = 'erp_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function clearAuthState(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('erp_user')
}

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

instance.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (resp) => resp,
  (error) => {
    // 网络层错误统一提示（业务码错误在 http() 中处理）
    const msg = error?.response?.data?.msg || error?.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** 401 时清理本地态并跳登录页（带原地址，登录后回跳） */
function handleUnauthorized(): void {
  clearAuthState()
  if (!location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    location.href = `/login?redirect=${redirect}`
  }
}

/**
 * 统一请求：自动解包后端的 Result<T>，成功返回 data，失败抛错。
 * code=401 视为登录态失效，自动跳转登录页。
 */
function http<T>(config: AxiosRequestConfig): Promise<T> {
  return instance(config).then((resp) => {
    const body = resp.data as Result<T>
    if (body && body.code === 200) {
      return body.data
    }
    if (body && body.code === 401) {
      handleUnauthorized()
      return Promise.reject(new Error(body.msg || '请先登录'))
    }
    const msg = body?.msg || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  })
}

export default {
  get: <T>(url: string, params?: object) => http<T>({ url, method: 'get', params }),
  post: <T>(url: string, data?: unknown) => http<T>({ url, method: 'post', data }),
  put: <T>(url: string, data?: unknown) => http<T>({ url, method: 'put', data }),
  delete: <T>(url: string) => http<T>({ url, method: 'delete' })
}
