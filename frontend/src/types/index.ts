export interface Result<T> {
  code: number
  msg: string
  data: T
}

export interface UserVO {
  id: number
  username: string
  realName?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export type OrderStatus = 'draft' | 'pending' | 'shipped' | 'paid' | 'cancelled'

export interface Customer {
  id: number
  name: string
  contact?: string
  phone?: string
  creditLimit?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
  debtAmount?: number
  orderCount?: number
  overCredit?: boolean
}

export interface OrderVO {
  id: number
  orderNo: string
  customerOrderNo?: string
  customerId: number
  customerName?: string
  orderDate?: string
  totalAmount?: number
  status: OrderStatus
  statusLabel?: string
  expectedDelivery?: string
  dueDate?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
  paidAmount?: number
  balance?: number
  paidRatio?: number
  availableActions?: string[]
}

export interface Payment {
  id: number
  orderId: number
  amount: number
  receivedDate: string
  note?: string
  createdAt?: string
}

export interface Shipment {
  id: number
  orderId: number
  shipmentDate: string
  trackingNo?: string
  confirmed: number
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface OrderDetailVO {
  id: number
  orderNo: string
  customerOrderNo?: string
  customerId: number
  customerName?: string
  customerContact?: string
  customerPhone?: string
  orderDate?: string
  totalAmount?: number
  status: OrderStatus
  statusLabel?: string
  expectedDelivery?: string
  dueDate?: string
  remark?: string
  paidAmount?: number
  balance?: number
  paidRatio?: number
  shipment?: Shipment
  payments?: Payment[]
  availableActions?: string[]
}

export interface AgingBucket {
  bucket: string
  label: string
  amount: number
  count: number
}

export interface DashboardVO {
  draftAmount: number
  monthOrderAmount: number
  receivable: number
  monthDueAmount: number
  notDueAmount: number
  totalPaid: number
  monthReceived: number
  receivableOrderCount: number
  customerCount: number
  aging: AgingBucket[]
  topDebtCustomers: Customer[]
  statDate: string
}

export interface StatementOrder {
  id: number
  orderNo: string
  customerOrderNo?: string
  orderDate?: string
  totalAmount: number
  status: OrderStatus
  statusLabel?: string
  shipmentDate?: string
  trackingNo?: string
  paidAmount: number
  balance: number
  payments: Payment[]
}

export interface StatementVO {
  customerId: number
  customerName: string
  from: string
  to: string
  openingDebt: number
  periodShippedAmount: number
  periodReceivedAmount: number
  closingDebt: number
  orders: StatementOrder[]
  otherPayments: Payment[]
}

export interface MonthTrend {
  month: string
  orderCount: number
  shipmentCount: number
  receivedAmount: number
  completionRate: number
}

export interface MonthlyReportVO {
  month: string
  orderCount: number
  shipmentCount: number
  receivedAmount: number
  completionRate: number
  trend: MonthTrend[]
  monthOrders: Array<Record<string, unknown>>
}
