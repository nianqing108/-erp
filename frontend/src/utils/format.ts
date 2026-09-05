import type { OrderStatus } from '@/types'

/** 金额格式化：千分位 + 2 位小数 */
export function money(v?: number | string | null): string {
  const n = Number(v ?? 0)
  if (!isFinite(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 金额转数值（用于计算），缺省 0 */
export function toNum(v?: number | string | null): number {
  const n = Number(v ?? 0)
  return isFinite(n) ? n : 0
}

/** 状态元数据：中文名 + el-tag 语义色（info灰/primary蓝/success绿/danger红/warning橙） */
export const STATUS_META: Record<OrderStatus, { label: string; tagType: 'info' | 'primary' | 'success' | 'danger' | 'warning' }> = {
  draft: { label: '待发货', tagType: 'warning' },
  pending: { label: '待出货', tagType: 'info' },
  shipped: { label: '待付款', tagType: 'primary' },
  paid: { label: '已完成', tagType: 'success' },
  cancelled: { label: '已取消', tagType: 'danger' }
}

export function statusLabel(status: OrderStatus): string {
  return STATUS_META[status]?.label ?? status
}

export function statusTagType(status: OrderStatus): 'info' | 'primary' | 'success' | 'danger' | 'warning' {
  return STATUS_META[status]?.tagType ?? 'info'
}
