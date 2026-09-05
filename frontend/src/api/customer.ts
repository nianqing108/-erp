import request from './request'
import type { Customer, PageResult } from '@/types'

export interface CustomerQuery {
  keyword?: string
  onlyDebt?: boolean
  pageNum?: number
  pageSize?: number
}

export const pageCustomers = (q: CustomerQuery) =>
  request.get<PageResult<Customer>>('/customers', q)

export const allCustomers = () => request.get<Customer[]>('/customers/all')

export const getCustomer = (id: number) => request.get<Customer>(`/customers/${id}`)

export const createCustomer = (data: Partial<Customer>) =>
  request.post<number>('/customers', data)

export const updateCustomer = (id: number, data: Partial<Customer>) =>
  request.put<void>(`/customers/${id}`, data)

export const deleteCustomer = (id: number) => request.delete<void>(`/customers/${id}`)
