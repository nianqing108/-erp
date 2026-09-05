import request from './request'
import type { OrderVO, OrderDetailVO, PageResult } from '@/types'

export interface OrderQuery {
  status?: string
  customerId?: number
  orderNo?: string
  customerOrderNo?: string
  dateFrom?: string
  dateTo?: string
  pageNum?: number
  pageSize?: number
}

export const pageOrders = (q: OrderQuery) => request.get<PageResult<OrderVO>>('/orders', { params: q })

export const getOrder = (id: number) => request.get<OrderDetailVO>(`/orders/${id}`)

export const createOrder = (data: {
  customerId: number
  orderDate: string
  totalAmount: number
  expectedDelivery?: string
  dueDate?: string
  customerOrderNo?: string
  remark?: string
}) => request.post<{ id: number; orderNo: string }>('/orders', data)

export const updateOrder = (id: number, data: {
  customerId: number
  orderDate: string
  totalAmount: number
  expectedDelivery?: string
  dueDate?: string
  customerOrderNo?: string
  remark?: string
}) => request.put<void>(`/orders/${id}`, data)

/** 直接发货：draft → shipped，shipmentDate 为实际发货日 */
export const shipOrder = (id: number, data: { shipmentDate: string; trackingNo?: string; remark?: string }) =>
  request.post<void>(`/orders/${id}/ship`, data)

export const payOrder = (id: number, data: { amount: number; receivedDate: string; note?: string }) =>
  request.post<void>(`/orders/${id}/pay`, data)

export const cancelOrder = (id: number) => request.post<void>(`/orders/${id}/cancel`)

export const checkPo = (customerId: number, customerOrderNo: string, excludeOrderId?: number) =>
  request.get<boolean>('/orders/check-po', { customerId, customerOrderNo, excludeOrderId })
