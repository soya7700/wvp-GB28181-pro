<template>
  <view class="page">
    <view class="score">
      <text class="score__value">{{ data?.averageScore ?? 0 }}</text>
      <text class="score__label">平均健康分</text>
    </view>
    <view class="metrics">
      <view><text>{{ data?.total ?? 0 }}</text><small>全部</small></view>
      <view class="ok"><text>{{ data?.healthy ?? 0 }}</text><small>健康</small></view>
      <view class="warn"><text>{{ data?.warning ?? 0 }}</text><small>关注</small></view>
      <view class="bad"><text>{{ data?.critical ?? 0 }}</text><small>严重</small></view>
    </view>
    <view class="title">问题点位</view>
    <view v-if="loading" class="empty">正在加载…</view>
    <view v-else-if="!data?.problemChannels.length" class="empty">暂无异常点位</view>
    <view v-for="item in data?.problemChannels" :key="item.id" class="card">
      <view class="row"><text class="name">{{ item.channelId }}</text><text class="badge" :class="item.healthStatus.toLowerCase()">{{ item.healthScore }} 分</text></view>
      <text class="desc">{{ item.diagnostic || diagnostic(item) }}</text>
      <text class="time">{{ item.checkTime }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getChannelHealth, type ChannelHealth, type HealthDashboard } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const data = ref<HealthDashboard>()
const loading = ref(false)
function diagnostic(item: ChannelHealth) {
  if (!item.online) return '设备离线'
  if (!item.streamAvailable) return '在线但无法取流'
  if (!item.recordingComplete) return '录像不完整'
  return '视频质量需要关注'
}
async function load() {
  loading.value = true
  try { data.value = await getChannelHealth() }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' }) }
  finally { loading.value = false }
}
onShow(() => { if (auth.requireAuth()) load() })
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.score{padding:40rpx;text-align:center;color:#fff;background:linear-gradient(135deg,#175cd3,#1570ef);border-radius:28rpx}.score__value,.score__label{display:block}.score__value{font-size:72rpx;font-weight:700}.score__label{margin-top:8rpx;font-size:24rpx;opacity:.8}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:10rpx;margin-top:18rpx}.metrics view{padding:20rpx 4rpx;text-align:center;background:#fff;border-radius:18rpx}.metrics text,.metrics small{display:block}.metrics text{font-size:32rpx;font-weight:700}.metrics small{margin-top:6rpx;color:#8490a5;font-size:20rpx}.metrics .ok text{color:#079455}.metrics .warn text{color:#dc6803}.metrics .bad text{color:#d92d20}.title{margin:30rpx 4rpx 16rpx;font-size:30rpx;font-weight:700}.card{margin-bottom:14rpx;padding:26rpx;background:#fff;border-radius:20rpx}.row{display:flex;align-items:center;justify-content:space-between}.name{font-size:28rpx;font-weight:650}.badge{padding:7rpx 14rpx;border-radius:99rpx;font-size:21rpx}.badge.warning{color:#b54708;background:#ffead5}.badge.critical{color:#b42318;background:#fee4e2}.desc,.time{display:block}.desc{margin-top:16rpx;color:#475467;font-size:24rpx}.time{margin-top:10rpx;color:#98a2b3;font-size:21rpx}.empty{padding:80rpx;text-align:center;color:#98a2b3}
</style>
