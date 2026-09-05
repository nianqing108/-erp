<template>
  <div class="page">
    <div class="toolbar glass">
      <el-input v-model="keyword" placeholder="搜索公司名称 / 联系人 / 电话" clearable style="width: 280px"
        @keyup.enter="load" @clear="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-switch v-model="onlyDebt" active-text="仅看有欠款" @change="load" />
      <div class="spacer" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增客户</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" class="glass table" stripe>
      <el-table-column prop="name" label="公司名称" min-width="160" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column label="信用额度" width="140" align="right">
        <template #default="{ row }"><span class="num">{{ money(row.creditLimit) }}</span></template>
      </el-table-column>
      <el-table-column label="当前欠款" width="150" align="right">
        <template #default="{ row }">
          <span class="num" :class="{ danger: row.overCredit }">{{ money(row.debtAmount) }}</span>
          <el-tag v-if="row.overCredit" type="danger" size="small" effect="dark" style="margin-left: 6px">超额</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="goStatement(row)">对账单</el-button>
          <el-popconfirm title="确认删除该客户？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load"
        @size-change="load" />
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑客户' : '新增客户'" width="460px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="公司名称" required>
          <el-input v-model="form.name" placeholder="必填，唯一" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="信用额度">
          <el-input v-model.number="form.creditLimit" type="number" placeholder="0 表示不限制">
            <template #append>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  pageCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  type CustomerQuery
} from '@/api/customer'
import type { Customer } from '@/types'
import { money } from '@/utils/format'

const router = useRouter()
const rows = ref<Customer[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const onlyDebt = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)

const dialog = ref(false)
const editing = ref<Customer | null>(null)
const saving = ref(false)
const form = reactive<Partial<Customer>>({})

async function load() {
  loading.value = true
  try {
    const q: CustomerQuery = {
      keyword: keyword.value || undefined,
      onlyDebt: onlyDebt.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    const data = await pageCustomers(q)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', contact: '', phone: '', creditLimit: 0, remark: '' })
  dialog.value = true
}

function openEdit(row: Customer) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    contact: row.contact,
    phone: row.phone,
    creditLimit: row.creditLimit ?? 0,
    remark: row.remark
  })
  dialog.value = true
}

async function save() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写公司名称')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, creditLimit: Number(form.creditLimit ?? 0) }
    if (editing.value) {
      await updateCustomer(editing.value.id, payload)
      ElMessage.success('已保存')
    } else {
      await createCustomer(payload)
      ElMessage.success('客户已创建')
    }
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Customer) {
  try {
    await deleteCustomer(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    /* 错误消息已由拦截器提示 */
  }
}

function goStatement(row: Customer) {
  router.push({ name: 'statement', query: { customerId: String(row.id) } })
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 14px;
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
