<template>
  <view class="page">
    <view class="notice">设备操作会依据协议能力显示，不支持的功能不会提供入口。</view>
    <view v-if="!items.length" class="empty">暂无移动记录仪</view>
    <view v-for="item in items" :key="item.id" class="card">
      <view class="head"><text class="title">{{ item.name }}</text><text class="badge">{{ statusName(item.status) }}</text></view>
      <text class="meta">{{ item.deviceCode }} · {{ item.vendor || '通用设备' }} {{ item.model || '' }}</text>
      <view class="health"><text>网络 {{ item.networkStatus }}</text><text>电量 {{ item.batteryLevel ?? '-' }}%</text><text>存储 {{ item.storagePercent ?? '-' }}%</text></view>
      <view class="capabilities"><text v-for="cap in capabilities(item)" :key="cap">{{ capabilityName(cap) }}</text></view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { queryMobileRecorders, type MobileRecorder } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), items = ref<MobileRecorder[]>([])
function capabilities(item: MobileRecorder) { return (item.capabilities || '').split(',').filter(Boolean) }
function capabilityName(code: string) { return { LIVE_VIDEO:'实时视频', AUDIO_LISTEN:'声音监听', INTERCOM:'语音对讲', SNAPSHOT:'远程拍照', RECORD_CONTROL:'录像控制', LOCATION:'定位', FILE_UPLOAD:'文件上传' }[code] || code }
function statusName(status: string) { return { AVAILABLE:'可领用', ASSIGNED:'已领用', DISABLED:'已停用' }[status] || status }
onShow(async () => { if (auth.requireAuth()) items.value = await queryMobileRecorders() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.notice{margin-bottom:18rpx;padding:20rpx;color:#175cd3;background:#eff8ff;border-radius:18rpx;font-size:22rpx}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;justify-content:space-between}.title{font-size:29rpx;font-weight:650}.badge{color:#027a48;font-size:21rpx}.meta{display:block;margin-top:10rpx;color:#667085;font-size:22rpx}.health{display:flex;gap:22rpx;margin-top:18rpx;color:#475467;font-size:21rpx}.capabilities{display:flex;flex-wrap:wrap;gap:10rpx;margin-top:18rpx}.capabilities text{padding:6rpx 12rpx;color:#344054;background:#f2f4f7;border-radius:10rpx;font-size:19rpx}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
