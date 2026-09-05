<template>
  <div class="page">
    <div class="toolbar glass">
      <span class="tb-label">统计月份</span>
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="load" />
      <div class="spacer" />
      <el-button :disabled="!month" @click="doExport">导出 Excel</el-button>
    </div>

    <div class="metric-grid">
      <div class="glass metric hover-up"><div class="m-label">订单量</div><div class="m-value num">{{ stmt?.orderCount ?? 0 }}</div><div class="m-sub">本期下单（不含取消）</div></div>
      <div class="glass metric hover-up"><div class="m-label">出货量</div><div class="m-value num">{{ stmt?.shipmentCount ?? 0 }}</div><div class="m-sub">已确认发货笔数</div></div>
      <div class="glass metric hover-up"><div class="m-label">回款金额</div><div class="m-value num">{{ money(stmt?.receivedAmount) }}</div><div class="m-sub">本期到账</div></div>
      <div class="glass metric accent hover-up"><div class="m-label">订单完成率</div><div class="m-value num">{{ (stmt?.completionRate ?? 0).toFixed(1) }}%</div><div class="m-sub">已完成 / 下单</div></div>
    </div>

    <div class="glass panel">
      <div class="panel-title">近 12 月趋势</div>
      <VChart :option="trendOption" autoresize class="chart" />
    </div>

    <div class="glass panel">
      <div class="panel-title">本月订单明细</div>
      <el-table :data="stmt?.monthOrders ?? []" v-loading="loading">
        <el-table-column prop="orderNo" label="内部订单号" width="170" />
        <el-table-column prop="customerOrderNo" label="客户订单号" width="150" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
        <el-table-column prop="orderDate" label="下单日" width="120" />
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" effect="light" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getMonthly, exportMonthlyUrl } from '@/api/report'
import type { MonthlyReportVO } from '@/types'
import { money, statusLabel, statusTagType } from '@/utils/format'

const month = ref(currentMonth())
const loading = ref(false)
const stmt = ref<MonthlyReportVO | null>(null)

function currentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const trendOption = computed(() => {
  const trend = stmt.value?.trend ?? []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单量', '出货量', '回款金额'], bottom: 0 },
    grid: { left: 50, right: 60, top: 30, bottom: 50 },
    xAxis: { type: 'category', data: trend.map((t) => t.month) },
    yAxis: [
      { type: 'value', name: '笔数' },
      { type: 'value', name: '回款(元)' }
    ],
    series: [
      { name: '订单量', type: 'bar', data: trend.map((t) => t.orderCount), itemStyle: { color: '#2563eb', borderRadius: [4, 4, 0, 0] } },
      { name: '出货量', type: 'bar', data: trend.map((t) => t.shipmentCount), itemStyle: { color: '#60a5fa', borderRadius: [4, 4, 0, 0] } },
      { name: '回款金额', type: 'line', yAxisIndex: 1, smooth: true, data: trend.map((t) => t.receivedAmount), itemStyle: { color: '#16a34a' }, lineStyle: { width: 3 } }
    ]
  }
})

async function load() {
  if (!month.value) return
  loading.value = true
  try {
    stmt.value = await getMonthly(month.value)
  } finally {
    loading.value = false
  }
}

function doExport() {
  if (!month.value) return
  window.open(exportMonthlyUrl(month.value), '_blank')
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 14px;
}
.tb-label {
  color: var(--c-text-sub);
  font-size: 13px;
}
.spacer {
  flex: 1;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}
.metric {
  padding: 18px 20px;
  border-radius: 16px;
}
.metric.accent {
  background: linear-gradient(135deg, rgba(22, 163, 74, 0.14), rgba(96, 165, 250, 0.06));
  border-color: rgba(22, 163, 74, 0.22);
}
.metric.accent .m-value {
  color: #16a34a;
}
.m-label {
  color: var(--c-text-sub);
  font-size: 13px;
}
.m-value {
  font-size: 26px;
  font-weight: 700;
  margin: 8px 0 2px;
}
.m-sub {
  font-size: 12px;
  color: var(--c-text-mute);
}
.panel {
  padding: 16px 18px;
  border-radius: 16px;
  margin-bottom: 14px;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 10px;
}
.chart {
  height: 320px;
}
@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
