<template>
  <view class="page">
    <view v-if="loading" class="empty">正在加载…</view>
    <view v-else-if="!items.length" class="empty">暂无巡检工单</view>
    <view v-for="item in items" :key="item.id" class="card">
      <view class="head"><text class="title">{{ item.title }}</text><text class="status" :class="item.status.toLowerCase()">{{ statusText[item.status] }}</text></view>
      <text class="meta">优先级：{{ item.priority }}　截止：{{ item.dueTime }}</text>
      <text v-if="item.resolution" class="resolution">{{ item.resolution }}</text>
      <view class="actions">
        <button v-if="item.status === 'OPEN'" size="mini" class="primary" @click="accept(item.id)">接单</button>
        <button v-if="item.status === 'PROCESSING'" size="mini" class="primary" @click="resolve(item.id)">提交解决</button>
        <button v-if="item.status === 'RESOLVED'" size="mini" @click="verify(item.id, false)">复检失败</button>
        <button v-if="item.status === 'RESOLVED'" size="mini" class="primary" @click="verify(item.id, true)">复检通过</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { acceptWorkOrder, queryWorkOrders, resolveWorkOrder, verifyWorkOrder, type InspectionWorkOrder } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const items = ref<InspectionWorkOrder[]>([])
const loading = ref(false)
const statusText = { OPEN: '待接单', PROCESSING: '处理中', RESOLVED: '待复检', CLOSED: '已完成' }
async function load() { loading.value = true; try { items.value = await queryWorkOrders() } finally { loading.value = false } }
async function accept(id: number) { await acceptWorkOrder(id); await load() }
function resolve(id: number) {
  uni.showModal({ title: '提交解决结果', editable: true, placeholderText: '填写处理情况', success: async result => {
    if (result.confirm && result.content) { await resolveWorkOrder(id, result.content); await load() }
  } })
}
async function verify(id: number, passed: boolean) { await verifyWorkOrder(id, passed); await load() }
onShow(() => { if (auth.requireAuth()) load() })
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;align-items:flex-start;justify-content:space-between;gap:16rpx}.title{font-size:28rpx;font-weight:650}.status{flex:none;padding:7rpx 14rpx;border-radius:99rpx;font-size:20rpx;color:#175cd3;background:#eff4ff}.status.closed{color:#027a48;background:#ecfdf3}.status.resolved{color:#b54708;background:#fffaeb}.meta,.resolution{display:block;margin-top:16rpx;font-size:22rpx;color:#667085}.resolution{padding:16rpx;background:#f8fafc;border-radius:12rpx}.actions{display:flex;justify-content:flex-end;gap:12rpx;margin-top:20rpx}.actions button{margin:0}.actions .primary{color:#fff;background:#1677ff}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
