<template>
  <div class="page">
    <div class="toolbar glass">
      <span class="tb-label">客户</span>
      <el-select v-model="customerId" filterable placeholder="选择客户" style="width: 220px" @change="load">
        <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <span class="tb-label">对账区间</span>
      <el-date-picker v-model="from" type="date" value-format="YYYY-MM-DD" placeholder="起始" style="width: 150px" @change="load" />
      <span class="tb-sep">~</span>
      <el-date-picker v-model="to" type="date" value-format="YYYY-MM-DD" placeholder="结束" style="width: 150px" @change="load" />
      <div class="spacer" />
      <el-button type="primary" :disabled="!customerId" :loading="loading" @click="load">查询</el-button>
      <el-button :disabled="!customerId" @click="doExport">导出 Excel</el-button>
    </div>

    <div v-if="stmt" class="summary">
      <div class="glass sum-cell"><div class="sum-label">期初欠款</div><div class="num">{{ money(stmt.openingDebt) }}</div></div>
      <div class="glass sum-cell"><div class="sum-label">本期发货额</div><div class="num">{{ money(stmt.periodShippedAmount) }}</div></div>
      <div class="glass sum-cell"><div class="sum-label">本期回款额</div><div class="num">{{ money(stmt.periodReceivedAmount) }}</div></div>
      <div class="glass sum-cell accent"><div class="sum-label">期末欠款</div><div class="num">{{ money(stmt.closingDebt) }}</div></div>
    </div>

    <div class="glass panel">
      <div class="panel-title">
        发货订单明细
        <span class="text-mute">（共 {{ stmt?.orders.length ?? 0 }} 笔）</span>
      </div>
      <el-table :data="stmt?.orders ?? []" v-loading="loading" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row.payments || []" size="small" class="inner">
              <el-table-column prop="receivedDate" label="到账日" width="120" />
              <el-table-column prop="amount" label="金额" align="right" width="140">
                <template #default="{ r }"><span class="num">{{ money(r.amount) }}</span></template>
              </el-table-column>
              <el-table-column prop="note" label="备注" show-overflow-tooltip />
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="内部订单号" width="160" />
        <el-table-column prop="customerOrderNo" label="客户订单号" width="140" show-overflow-tooltip />
        <el-table-column prop="orderDate" label="下单日" width="120" />
        <el-table-column prop="shipmentDate" label="发货日" width="120" />
        <el-table-column prop="trackingNo" label="物流单号" width="130" show-overflow-tooltip />
        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="已收" width="130" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.paidAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="未收" width="130" align="right">
          <template #default="{ row }"><span class="num" :class="{ danger: toNum(row.balance) > 0 }">{{ money(row.balance) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>

      <div v-if="stmt && stmt.otherPayments.length" class="other">
        <div class="panel-title">本期其他回款（前期发货订单在本期回款）</div>
        <el-table :data="stmt.otherPayments" size="small">
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column prop="receivedDate" label="到账日" width="130" />
          <el-table-column prop="amount" label="金额" align="right" width="150">
            <template #default="{ r }"><span class="num">{{ money(r.amount) }}</span></template>
          </el-table-column>
          <el-table-column prop="note" label="备注" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { allCustomers } from '@/api/customer'
import { getStatement, exportStatementUrl } from '@/api/report'
import type { Customer, StatementVO } from '@/types'
import { money, toNum, statusLabel, statusTagType } from '@/utils/format'

const route = useRoute()
const customers = ref<Customer[]>([])
const customerId = ref<number | undefined>(undefined)
const from = ref(firstOfMonth())
const to = ref(todayStr())
const loading = ref(false)
const stmt = ref<StatementVO | null>(null)

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function firstOfMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

async function load() {
  if (!customerId.value) {
    stmt.value = null
    return
  }
  loading.value = true
  try {
    stmt.value = await getStatement(customerId.value, from.value, to.value)
  } finally {
    loading.value = false
  }
}

function doExport() {
  if (!customerId.value) return
  window.open(exportStatementUrl(customerId.value, from.value, to.value), '_blank')
}

watch(
  () => route.query.customerId,
  (v) => {
    if (v) {
      customerId.value = Number(v)
      load()
    }
  },
  { immediate: true }
)

onMounted(async () => {
  customers.value = await allCustomers()
  const q = route.query.customerId
  if (q) {
    customerId.value = Number(q)
  }
  if (customerId.value) load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 14px;
  flex-wrap: wrap;
}
.tb-label {
  color: var(--c-text-sub);
  font-size: 13px;
}
.tb-sep {
  color: var(--c-text-mute);
}
.spacer {
  flex: 1;
}
.summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}
.sum-cell {
  padding: 16px 18px;
  border-radius: 14px;
  text-align: center;
}
.sum-cell.accent {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(96, 165, 250, 0.08));
  border-color: rgba(37, 99, 235, 0.25);
}
.sum-cell.accent .num {
  color: var(--c-primary);
}
.sum-label {
  color: var(--c-text-sub);
  font-size: 13px;
  margin-bottom: 6px;
}
.sum-cell .num {
  font-size: 22px;
  font-weight: 700;
}
.panel {
  padding: 16px 18px;
  border-radius: 16px;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 10px;
}
.inner {
  margin: 0 12px;
}
.other {
  margin-top: 16px;
}
@media (max-width: 1200px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
