import request from './request'
import type { DashboardVO } from '@/types'

export const getDashboard = () => request.get<DashboardVO>('/dashboard/overview')
