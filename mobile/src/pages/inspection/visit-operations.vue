<template>
  <view class="page">
    <view v-if="summary" class="summary">
      <view class="metric"><text>{{ summary.storeCount }}</text><small>覆盖门店</small></view>
      <view class="metric"><text>{{ percent(summary.completionRate) }}</text><small>任务完成率</small></view>
      <view class="metric"><text>{{ percent(summary.rectificationRate) }}</text><small>整改闭环率</small></view>
      <view class="metric danger"><text>{{ summary.overdueCount }}</text><small>逾期整改</small></view>
    </view>
    <text class="section-title">整改任务</text>
    <view v-if="!items.length" class="empty">暂无整改任务</view>
    <view v-for="item in items" :key="item.id" class="card">
      <view class="head"><text class="title">{{ item.title }}</text><text class="badge">{{ statusName(item.status) }}</text></view>
      <text class="meta">{{ item.storeName }} · 截止 {{ item.dueTime }}</text>
      <view v-if="item.status === 'SUBMITTED'" class="actions">
        <button size="mini" @click="review(item.id, true)">复核通过</button>
        <button size="mini" @click="review(item.id, false)">退回整改</button>
      </view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { queryVisitOperations, queryVisitRectifications, reviewVisitRectification, type VisitOperationsSummary, type VisitRectification } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), summary = ref<VisitOperationsSummary>(), items = ref<VisitRectification[]>([])
async function load() { [summary.value, items.value] = await Promise.all([queryVisitOperations(), queryVisitRectifications()]) }
function percent(v: number) { return `${Math.round((v || 0) * 100)}%` }
function statusName(v: string) { return { OPEN:'待整改', SUBMITTED:'待复核', REJECTED:'已退回', CLOSED:'已关闭' }[v] || v }
async function review(id: number, passed: boolean) { await reviewVisitRectification(id, passed, passed ? '' : '请补充清晰整改证据'); await load() }
onShow(() => { if (auth.requireAuth()) load() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.summary{display:grid;grid-template-columns:1fr 1fr;gap:14rpx}.metric{padding:26rpx;background:#fff;border-radius:20rpx}.metric text,.metric small{display:block}.metric text{font-size:38rpx;font-weight:700}.metric small{margin-top:8rpx;color:#667085;font-size:21rpx}.metric.danger text{color:#b42318}.section-title{display:block;margin:28rpx 4rpx 16rpx;font-size:29rpx;font-weight:650}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;justify-content:space-between}.title{font-size:27rpx;font-weight:650}.badge{color:#b54708;font-size:21rpx}.meta{display:block;margin-top:12rpx;color:#667085;font-size:22rpx}.actions{display:flex;gap:12rpx;margin-top:18rpx}.actions button{margin:0}.empty{padding:80rpx;text-align:center;color:#98a2b3}
</style>
