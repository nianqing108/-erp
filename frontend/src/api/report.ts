import request from './request'
import type { StatementVO, MonthlyReportVO } from '@/types'

export const getStatement = (customerId: number, from: string, to: string) =>
  request.get<StatementVO>(`/reports/statement/${customerId}`, { from, to })

export const getMonthly = (month: string) =>
  request.get<MonthlyReportVO>('/reports/monthly', { month })

const base = import.meta.env.VITE_API_BASE || '/api'

export const exportStatementUrl = (customerId: number, from: string, to: string) =>
  `${base}/reports/export/statement/${customerId}?from=${from}&to=${to}`

export const exportMonthlyUrl = (month: string) => `${base}/reports/export/monthly?month=${month}`
