<template>
  <view class="page">
    <view v-if="!items.length" class="empty">暂无巡店任务</view>
    <view v-for="item in items" :key="item.id" class="card">
      <view class="head"><text class="title">{{ item.title }}</text><text class="badge">{{ statusName(item.status) }}</text></view>
      <text class="store">{{ item.storeName }}</text>
      <text class="time">{{ item.plannedStartTime }} - {{ item.plannedEndTime }}</text>
      <button v-if="item.status === 'PENDING'" size="mini" @click="checkin(item.id)">到店签到</button>
      <button v-if="item.status === 'IN_PROGRESS'" size="mini" @click="checkout(item.id)">完成并签退</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { checkinStoreVisit, checkoutStoreVisit, queryStoreVisits, type StoreVisitTask } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), items = ref<StoreVisitTask[]>([])
async function load() { items.value = await queryStoreVisits() }
function statusName(status: string) { return { PENDING:'待执行', IN_PROGRESS:'巡店中', COMPLETED:'已完成', CANCELLED:'已取消' }[status] || status }
function checkin(id: number) { uni.getLocation({ type:'gcj02', success: async p => { await checkinStoreVisit(id, p.longitude, p.latitude); await load() } }) }
async function checkout(id: number) { await checkoutStoreVisit(id); await load() }
onShow(() => { if (auth.requireAuth()) load() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;justify-content:space-between}.title{font-size:29rpx;font-weight:650}.badge{color:#175cd3;font-size:21rpx}.store,.time{display:block;margin-top:12rpx;color:#667085;font-size:23rpx}.card button{margin:20rpx 0 0;color:#fff;background:#175cd3}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
