<template>
  <div class="page">
    <div class="toolbar glass">
      <div class="filter-row">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="load">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="customerId" placeholder="全部客户" clearable filterable style="width: 180px" @change="load">
          <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-input v-model="orderNo" placeholder="内部订单号" clearable style="width: 160px" @keyup.enter="load" @clear="load" />
        <el-input v-model="customerOrderNo" placeholder="客户订单号" clearable style="width: 160px" @keyup.enter="load" @clear="load" />
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="~"
          start-placeholder="下单起" end-placeholder="下单止" style="width: 240px" @change="load" />
        <div class="spacer" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新建订单</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" class="glass table" stripe>
      <el-table-column prop="orderNo" label="内部订单号" width="160" />
      <el-table-column prop="customerOrderNo" label="客户订单号" width="140" show-overflow-tooltip />
      <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
      <el-table-column label="金额" width="130" align="right">
        <template #default="{ row }"><span class="num">{{ money(row.totalAmount) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" effect="light" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderDate" label="下单日" width="120" />
      <el-table-column prop="expectedDelivery" label="期望发货" width="120" />
      <el-table-column label="未收余额" width="130" align="right">
        <template #default="{ row }">
          <span class="num" :class="{ danger: toNum(row.balance) > 0 }">{{ money(row.balance) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.availableActions?.includes('ship')" link type="warning" @click="openDetail(row, 'ship')">发货</el-button>
          <el-button v-if="row.availableActions?.includes('pay')" link type="success" @click="openDetail(row, 'pay')">收款</el-button>
          <el-button v-if="row.availableActions?.includes('cancel')" link type="danger" @click="openDetail(row, 'cancel')">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" />
    </div>

    <OrderDetailDrawer v-model="drawer" :order-id="currentId" @changed="load" />

    <!-- 新建订单 -->
    <el-dialog v-model="createDlg" title="新建订单" width="460px">
      <el-form label-width="92px">
        <el-form-item label="客户" required>
          <el-select v-model="createForm.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button link type="primary" @click="goCustomer">去新增客户</el-button>
        </el-form-item>
        <el-form-item label="下单日期" required>
          <el-date-picker v-model="createForm.orderDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disableFuture" />
        </el-form-item>
        <el-form-item label="订单金额" required>
          <el-input v-model.number="createForm.totalAmount" type="number"><template #append>元</template></el-input>
        </el-form-item>
        <el-form-item label="客户订单号">
          <el-input v-model="createForm.customerOrderNo" placeholder="选填，客户提供的 PO 号" />
        </el-form-item>
        <el-form-item label="期望发货">
          <el-date-picker v-model="createForm.expectedDelivery" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="约定收款日">
          <el-date-picker v-model="createForm.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="选填，与客户约定的最晚回款日" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import OrderDetailDrawer from './OrderDetailDrawer.vue'
import { pageOrders, createOrder, type OrderQuery } from '@/api/order'
import { allCustomers } from '@/api/customer'
import type { Customer, OrderStatus, OrderVO } from '@/types'
import { money, toNum, statusLabel, statusTagType } from '@/utils/format'

const router = useRouter()
const rows = ref<OrderVO[]>([])
const total = ref(0)
const loading = ref(false)
const customers = ref<Customer[]>([])

const status = ref<string>('')
const customerId = ref<number | undefined>(undefined)
const orderNo = ref('')
const customerOrderNo = ref('')
const dateRange = ref<[string, string] | null>(null)
const pageNum = ref(1)
const pageSize = ref(20)

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'draft', label: '待发货' },
  { value: 'shipped', label: '待付款' },
  { value: 'paid', label: '已完成' },
  { value: 'cancelled', label: '已取消' }
]

const drawer = ref(false)
const currentId = ref<number | null>(null)
const createDlg = ref(false)
const creating = ref(false)
const createForm = reactive<any>({})

const today = computed(() => new Date())

/** 禁止选择未来日期（下单日期） */
function disableFuture(d: Date): boolean {
  return d.getTime() > Date.now()
}

async function load() {
  loading.value = true
  try {
    const q: OrderQuery = {
      status: status.value || undefined,
      customerId: customerId.value,
      orderNo: orderNo.value || undefined,
      customerOrderNo: customerOrderNo.value || undefined,
      dateFrom: dateRange.value?.[0],
      dateTo: dateRange.value?.[1],
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const data = await pageOrders(q)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openDetail(row: OrderVO, _focus?: string) {
  currentId.value = row.id
  drawer.value = true
}

function openCreate() {
  Object.assign(createForm, {
    customerId: undefined,
    orderDate: new Date().toISOString().slice(0, 10),
    totalAmount: undefined,
    customerOrderNo: '',
    expectedDelivery: '',
    dueDate: '',
    remark: ''
  })
  createDlg.value = true
}

async function doCreate() {
  if (!createForm.customerId) {
    ElMessage.warning('请选择客户')
    return
  }
  if (!createForm.orderDate) {
    ElMessage.warning('请选择下单日期')
    return
  }
  if (!createForm.totalAmount || createForm.totalAmount <= 0) {
    ElMessage.warning('订单金额必须大于 0')
    return
  }
  creating.value = true
  try {
    const r = await createOrder({
      customerId: createForm.customerId,
      orderDate: createForm.orderDate,
      totalAmount: Number(createForm.totalAmount),
      expectedDelivery: createForm.expectedDelivery || undefined,
      dueDate: createForm.dueDate || undefined,
      customerOrderNo: createForm.customerOrderNo || undefined,
      remark: createForm.remark || undefined
    })
    ElMessage.success(`订单已创建：${r.orderNo}`)
    createDlg.value = false
    load()
  } finally {
    creating.value = false
  }
}

function goCustomer() {
  createDlg.value = false
  router.push({ name: 'customers' })
}

onMounted(async () => {
  customers.value = await allCustomers()
  load()
})
</script>

<style scoped>
.toolbar {
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 14px;
}
.filter-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.spacer {
  flex: 1;
}
.table {
  border-radius: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
