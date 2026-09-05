<template>
  <el-drawer :model-value="modelValue" :title="title" size="520px" @update:model-value="emit('update:modelValue', $event)"
    @open="fetch">
    <div v-loading="loading">
      <div v-if="detail">
        <!-- 状态步骤：paid 时 active 越过末步索引，使全部步骤呈绿色完成态 -->
        <el-steps :active="stepActive" align-center finish-status="success" :process-status="cancelled ? 'error' : 'process'" class="steps">
          <el-step title="录入" />
          <el-step title="待发货" />
          <el-step title="待付款" />
          <el-step title="已完成" />
        </el-steps>
        <el-alert v-if="cancelled" type="error" :closable="false" title="订单已取消" show-icon style="margin: 10px 0" />

        <!-- 概要 -->
        <div class="block glass">
          <div class="kv"><span>内部订单号</span><b class="num">{{ detail.orderNo }}</b></div>
          <div class="kv"><span>客户订单号</span><b>{{ detail.customerOrderNo || '—' }}</b></div>
          <div class="kv"><span>客户</span><b>{{ detail.customerName }}</b></div>
          <div class="kv"><span>下单日期</span><b>{{ detail.orderDate }}</b></div>
          <div class="kv"><span>期望发货</span><b>{{ detail.expectedDelivery || '—' }}</b></div>
          <div class="kv"><span>约定收款日</span><b>{{ detail.dueDate || '—' }}</b></div>
        </div>

        <!-- 金额 -->
        <div class="block glass amount">
          <div class="amt-cell"><div class="amt-label">订单总额</div><div class="amt-val num">{{ money(detail.totalAmount) }}</div></div>
          <div class="amt-cell"><div class="amt-label">已收金额</div><div class="amt-val num">{{ money(detail.paidAmount) }}</div></div>
          <div class="amt-cell"><div class="amt-label">未收余额</div><div class="amt-val num danger" v-if="toNum(detail.balance) > 0">{{ money(detail.balance) }}</div>
            <div class="amt-val num" v-else>0.00</div></div>
        </div>
        <el-progress :percentage="clampPct(detail.paidRatio)" :stroke-width="10" color="#2563eb" class="bar" />

        <!-- 出货 -->
        <div class="block glass">
          <div class="block-title">出货信息</div>
          <template v-if="detail.shipment">
            <div class="kv"><span>发货日期</span><b>{{ detail.shipment.shipmentDate }}</b></div>
            <div class="kv"><span>物流单号</span><b>{{ detail.shipment.trackingNo || '—' }}</b></div>
            <div class="kv"><span>状态</span>
              <el-tag type="primary" effect="light" size="small">已发货</el-tag>
            </div>
          </template>
          <el-empty v-else description="尚未发货" :image-size="60" />
        </div>

        <!-- 收款流水 -->
        <div class="block glass">
          <div class="block-title">收款流水</div>
          <el-table :data="detail.payments || []" size="small" v-if="detail.payments?.length">
            <el-table-column prop="receivedDate" label="到账日" width="120" />
            <el-table-column prop="amount" label="金额" align="right">
              <template #default="{ row }"><span class="num">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
          </el-table>
          <el-empty v-else description="暂无收款记录" :image-size="60" />
        </div>

        <!-- 操作 -->
        <div class="actions">
          <el-button v-if="can('edit')" @click="openEdit">编辑</el-button>
          <el-button v-if="can('ship')" type="warning" @click="openShip">确认发货</el-button>
          <el-button v-if="can('pay')" type="success" @click="openPay">录入收款</el-button>
          <el-popconfirm v-if="can('cancel')" title="确认取消该订单？取消后不计入欠款" @confirm="doCancel">
            <template #reference><el-button type="danger" plain>取消订单</el-button></template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- 编辑 -->
    <el-dialog v-model="editDlg" title="编辑订单" width="440px" append-to-body>
      <el-form label-width="88px">
        <el-form-item label="客户">
          <el-select v-model="editForm.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单日期"><el-date-picker v-model="editForm.orderDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="订单金额"><el-input v-model.number="editForm.totalAmount" type="number" /><template #append>元</template></el-form-item>
        <el-form-item label="客户订单号"><el-input v-model="editForm.customerOrderNo" /></el-form-item>
        <el-form-item label="期望发货"><el-date-picker v-model="editForm.expectedDelivery" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="约定收款日"><el-date-picker v-model="editForm.dueDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="editDlg = false">取消</el-button><el-button type="primary" :loading="acting" @click="doEdit">保存</el-button></template>
    </el-dialog>

    <!-- 确认发货（一步直达：登记实际发货日，形成应收） -->
    <el-dialog v-model="shipDlg" title="确认发货" width="420px" append-to-body>
      <el-alert type="info" :closable="false" title="发货后订单进入「待付款」，形成应收欠款，且不可再取消" style="margin-bottom: 14px" />
      <el-form label-width="88px">
        <el-form-item label="实际发货日"><el-date-picker v-model="shipForm.shipmentDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="shipForm.trackingNo" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shipDlg = false">取消</el-button><el-button type="primary" :loading="acting" @click="doShip">确定发货</el-button></template>
    </el-dialog>

    <!-- 收款 -->
    <el-dialog v-model="payDlg" title="录入收款" width="420px" append-to-body>
      <el-form label-width="88px">
        <el-form-item label="本次收款">
          <el-input v-model.number="payForm.amount" type="number">
            <template #append>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="到账日期"><el-date-picker v-model="payForm.receivedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="payForm.note" placeholder="银行回单号等" /></el-form-item>
        <el-alert v-if="remaining > 0" type="info" :closable="false"
          :title="`未收余额 ${money(remaining)} 元，本次最多可收 ${money(remaining)} 元`" />
        <el-alert v-else type="success" :closable="false" title="该订单已结清" />
      </el-form>
      <template #footer><el-button @click="payDlg = false">取消</el-button><el-button type="success" :loading="acting" @click="doPay">确认收款</el-button></template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getOrder,
  updateOrder,
  shipOrder,
  payOrder,
  cancelOrder
} from '@/api/order'
import { allCustomers } from '@/api/customer'
import type { Customer, OrderDetailVO, OrderStatus } from '@/types'
import { money, toNum } from '@/utils/format'

const props = defineProps<{ modelValue: boolean; orderId: number | null }>()
const emit = defineEmits<{
  'update:modelValue': [boolean]
  changed: []
}>()

const detail = ref<OrderDetailVO | null>(null)
const loading = ref(false)
const acting = ref(false)
const customers = ref<Customer[]>([])

const editDlg = ref(false)
const shipDlg = ref(false)
const payDlg = ref(false)

const editForm = reactive<any>({})
const shipForm = reactive<any>({})
const payForm = reactive<any>({})

const title = computed(() => (detail.value ? `订单详情 · ${detail.value.orderNo}` : '订单详情'))
const cancelled = computed(() => detail.value?.status === 'cancelled')
const stepActive = computed(() => {
  // 4 步：录入(0) → 待发货(1) → 待付款(2) → 已完成(3)
  // draft 停在「待发货」，paid 用 4 越过末步索引让全部步骤变绿
  const map: Record<OrderStatus, number> = { draft: 1, pending: 1, shipped: 2, paid: 4, cancelled: 1 }
  return detail.value ? map[detail.value.status] : 0
})
const remaining = computed(() => (detail.value ? toNum(detail.value.balance) : 0))

function can(action: string) {
  return detail.value?.availableActions?.includes(action) ?? false
}
function clampPct(v?: number) {
  const n = Number(v ?? 0)
  return Math.max(0, Math.min(100, n))
}

async function fetch() {
  if (!props.orderId) return
  loading.value = true
  try {
    detail.value = await getOrder(props.orderId)
  } finally {
    loading.value = false
  }
}

function afterAction() {
  emit('changed')
  fetch()
}

function openEdit() {
  if (!detail.value) return
  Object.assign(editForm, {
    customerId: detail.value.customerId,
    orderDate: detail.value.orderDate,
    totalAmount: toNum(detail.value.totalAmount),
    customerOrderNo: detail.value.customerOrderNo,
    expectedDelivery: detail.value.expectedDelivery,
    dueDate: detail.value.dueDate,
    remark: detail.value.remark
  })
  if (!customers.value.length) allCustomers().then((r) => (customers.value = r))
  editDlg.value = true
}
async function doEdit() {
  if (!props.orderId) return
  acting.value = true
  try {
    await updateOrder(props.orderId, { ...editForm, totalAmount: Number(editForm.totalAmount) })
    ElMessage.success('已保存')
    editDlg.value = false
    afterAction()
  } finally {
    acting.value = false
  }
}

function openShip() {
  Object.assign(shipForm, {
    shipmentDate: new Date().toISOString().slice(0, 10),
    trackingNo: detail.value?.shipment?.trackingNo || ''
  })
  shipDlg.value = true
}
async function doShip() {
  if (!props.orderId) return
  if (!shipForm.shipmentDate) {
    ElMessage.warning('请选择实际发货日')
    return
  }
  acting.value = true
  try {
    await shipOrder(props.orderId, shipForm)
    ElMessage.success('已发货，形成应收')
    shipDlg.value = false
    afterAction()
  } finally {
    acting.value = false
  }
}

function openPay() {
  Object.assign(payForm, { amount: '', receivedDate: '', note: '' })
  payDlg.value = true
}
async function doPay() {
  if (!props.orderId) return
  const amt = Number(payForm.amount)
  if (!amt || amt <= 0) {
    ElMessage.warning('请输入正确的收款金额')
    return
  }
  if (remaining.value > 0 && amt > remaining.value + 1e-6) {
    ElMessage.error(`本次收款不能超过未收余额 ${money(remaining.value)} 元`)
    return
  }
  acting.value = true
  try {
    await payOrder(props.orderId, payForm)
    ElMessage.success(amt >= remaining.value ? '收款成功，订单已结清' : '收款成功')
    payDlg.value = false
    afterAction()
  } finally {
    acting.value = false
  }
}

async function doCancel() {
  if (!props.orderId) return
  acting.value = true
  try {
    await cancelOrder(props.orderId)
    ElMessage.success('订单已取消')
    afterAction()
  } finally {
    acting.value = false
  }
}

watch(() => props.modelValue, (v) => {
  if (v) fetch()
})
</script>

<style scoped>
.steps {
  margin-bottom: 6px;
}
.block {
  padding: 14px 16px;
  border-radius: 14px;
  margin-top: 12px;
}
.block-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.kv {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
}
.kv span {
  color: var(--c-text-mute);
}
.amount {
  display: flex;
}
.amt-cell {
  flex: 1;
  text-align: center;
}
.amt-label {
  color: var(--c-text-mute);
  font-size: 12px;
}
.amt-val {
  font-size: 18px;
  font-weight: 600;
  margin-top: 4px;
}
.bar {
  margin: 10px 4px 0;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}
</style>
