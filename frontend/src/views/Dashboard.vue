<template>
  <div class="page">
    <div class="metric-grid">
      <div v-for="m in metrics" :key="m.label" class="glass metric hover-up" :class="{ accent: m.accent }">
        <div class="m-label">{{ m.label }}</div>
        <div class="m-value num">{{ m.integer ? m.value : money(m.value) }}</div>
        <div class="m-sub">{{ m.sub }}</div>
      </div>
    </div>

    <div class="row">
      <div class="glass panel aging-panel">
        <div class="panel-title">逾期账龄分布<span class="text-mute" style="font-size:12px;font-weight:400">（按约定收款日起算，未填的按发货日）</span></div>
        <VChart :option="agingOption" autoresize class="chart" />
      </div>

      <div class="glass panel debt-panel">
        <div class="panel-title">欠款 Top 客户</div>
        <div v-if="topDebt.length" class="debt-list">
          <div v-for="c in topDebt" :key="c.id" class="debt-item">
            <div class="debt-head">
              <span class="debt-name">{{ c.name }}</span>
              <span class="num" :class="{ danger: c.overCredit }">{{ money(c.debtAmount) }}</span>
            </div>
            <el-progress :percentage="pct(c)" :stroke-width="8" :color="c.overCredit ? '#dc2626' : '#2563eb'" />
            <div class="debt-foot text-mute" v-if="c.creditLimit && c.creditLimit > 0">
              信用额度 {{ money(c.creditLimit) }} · 占用 {{ creditPct(c) }}%
            </div>
          </div>
        </div>
        <el-empty v-else description="当前无应收欠款" :image-size="80" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboard } from '@/api/dashboard'
import type { DashboardVO } from '@/types'
import { money } from '@/utils/format'

const data = ref<DashboardVO | null>(null)

const metrics = computed(() => [
  { label: '未发货金额', value: data.value?.draftAmount ?? 0, sub: '已录入未发货', accent: false },
  { label: '本月订单额', value: data.value?.monthOrderAmount ?? 0, sub: '本月下单（不含取消）', accent: false },
  { label: '应收欠款', value: data.value?.receivable ?? 0, sub: `${data.value?.receivableOrderCount ?? 0} 笔未结清`, accent: true },
  { label: '本月到期金额', value: data.value?.monthDueAmount ?? 0, sub: '约定收款日在本月的未收余额', accent: true },
  { label: '未到期金额', value: data.value?.notDueAmount ?? 0, sub: '约定收款日尚未到达', accent: false },
  { label: '本月回款', value: data.value?.monthReceived ?? 0, sub: '本月到账', accent: false },
  { label: '累计已收', value: data.value?.totalPaid ?? 0, sub: '全部收款合计', accent: false },
  { label: '客户总数', value: data.value?.customerCount ?? 0, sub: '注册客户', accent: false, integer: true }
])

const agingOption = computed(() => {
  const buckets = data.value?.aging ?? []
  return {
    tooltip: { trigger: 'item', formatter: (p: any) => `${p.name}<br/>${money(p.value)} 元 (${p.percent}%)` },
    legend: { bottom: 0 },
    // 未到期灰蓝，逾期档由橙到深红递进
    color: ['#94a3b8', '#f59e0b', '#ea580c', '#dc2626', '#7f1d1d'],
    series: [
      {
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c}' },
        data: buckets.map((b) => ({ name: b.label, value: b.amount }))
      }
    ]
  }
})

const topDebt = computed(() => data.value?.topDebtCustomers ?? [])
const maxDebt = computed(() => Math.max(1, ...topDebt.value.map((c) => c.debtAmount ?? 0)))

function pct(c: { debtAmount?: number }) {
  return Math.round(((c.debtAmount ?? 0) / maxDebt.value) * 100)
}
function creditPct(c: { debtAmount?: number; creditLimit?: number }) {
  if (!c.creditLimit || c.creditLimit <= 0) return 0
  return Math.min(999, Math.round(((c.debtAmount ?? 0) / c.creditLimit) * 100))
}

onMounted(async () => {
  data.value = await getDashboard()
})
</script>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.metric {
  padding: 18px 20px;
  border-radius: 16px;
}
.metric.accent {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(96, 165, 250, 0.08));
  border-color: rgba(37, 99, 235, 0.25);
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
.metric.accent .m-value {
  color: var(--c-primary);
}
.m-sub {
  font-size: 12px;
  color: var(--c-text-mute);
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}
.panel {
  padding: 16px 18px;
  border-radius: 16px;
  min-height: 340px;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 10px;
}
.chart {
  height: 300px;
}
.debt-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 6px;
}
.debt-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.debt-name {
  font-weight: 500;
}
.debt-foot {
  font-size: 12px;
  margin-top: 4px;
}
@media (max-width: 1200px) {
  .metric-grid,
  .row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
