<template>
  <view class="page">
    <view class="hero"><text class="hello">你好，{{ auth.username || '管理员' }}</text><text class="desc">查看平台运行状态和常用入口</text></view>
    <view class="quick-grid">
      <view v-for="item in entries" :key="item.url" class="quick" @click="open(item)"><text class="quick__icon">{{ item.icon }}</text><text class="quick__name">{{ item.name }}</text></view>
    </view>
    <view v-if="monitor.favorites.length" class="section">
      <view class="section__head"><text>收藏监控</text><text class="refresh" @click="openMonitor">全部监控</text></view>
      <view class="favorite-grid"><view v-for="item in monitor.favorites.slice(0,4)" :key="item.deviceId+item.channelId" class="favorite" @click="play(item)"><text class="favorite__name">{{ item.name }}</text><text class="favorite__id">{{ item.channelId }}</text></view></view>
    </view>
    <view class="section"><view class="section__head"><text>平台概览</text><text class="refresh" @click="load">刷新</text></view>
      <view v-if="loading" class="loading">正在获取运行状态…</view>
      <view v-else class="info"><text class="info__label">系统状态</text><text class="info__value">{{ error ? '连接异常' : '运行中' }}</text><text v-if="error" class="error">{{ error }}</text></view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'; import { onShow } from '@dcloudio/uni-app'; import { useAuthStore } from '@/stores/auth'; import { useMonitorStore, type ChannelShortcut } from '@/stores/monitor'; import { getSystemInfo } from '@/api/server'
const auth = useAuthStore(); const monitor = useMonitorStore(); const loading = ref(false); const error = ref('')
const entries = [{ name: '视频监控', icon: '▣', url: '/pages/device/index', tab: true }, { name: 'AI 巡检', icon: 'AI', url: '/pages/inspection/index' }, { name: '站内消息', icon: '!', url: '/pages/message/index', tab: true }, { name: '录像查询', icon: '▶', url: '/pages/record/index', tab: true }, { name: '运维工具', icon: '◇', url: '/pages/proxy/index' }]
function open(item: typeof entries[number]) { item.tab ? uni.switchTab({ url: item.url }) : uni.navigateTo({ url: item.url }) }
async function load() { loading.value = true; error.value = ''; try { await getSystemInfo() } catch (e) { error.value = e instanceof Error ? e.message : '连接失败' } finally { loading.value = false } }
function openMonitor(){uni.switchTab({url:'/pages/device/index'})}
function play(item:ChannelShortcut){uni.navigateTo({url:`/pages/player/index?deviceId=${encodeURIComponent(item.deviceId)}&channelId=${encodeURIComponent(item.channelId)}&name=${encodeURIComponent(item.name)}`})}
onShow(() => { monitor.restore(); if (auth.requireAuth()) load() })
</script>
<style scoped>
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; }.hero { padding: 38rpx 34rpx; color: #fff; background: linear-gradient(135deg,#2368f2,#598cf4); border-radius: 28rpx; }.hello,.desc { display:block; }.hello { font-size: 38rpx; font-weight: 700; }.desc { margin-top: 12rpx; opacity:.82; font-size: 24rpx; }.quick-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:16rpx; margin-top:22rpx; }.quick { padding:26rpx 8rpx; display:flex; align-items:center; flex-direction:column; background:#fff; border-radius:22rpx; }.quick__icon { min-height:48rpx;color:#2368f2; font-size:34rpx;font-weight:700;line-height:48rpx }.quick__name { margin-top:12rpx; font-size:23rpx; }.section { margin-top:24rpx; padding:30rpx; background:#fff; border-radius:24rpx; }.section__head { display:flex; justify-content:space-between; font-size:30rpx; font-weight:600; }.refresh { color:#2368f2; font-size:25rpx; font-weight:400; }.loading,.info { padding:50rpx 0 20rpx; color:#7b8498; text-align:center; font-size:25rpx; }.info__label,.info__value { display:block; }.info__value { margin-top:12rpx; color:#172033; font-size:34rpx; font-weight:600; }.error { display:block; margin-top:12rpx; color:#d84b4b; }
.favorite-grid{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;margin-top:24rpx}.favorite{padding:22rpx;background:#f4f7fc;border-radius:18rpx}.favorite__name,.favorite__id{display:block;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}.favorite__name{font-size:26rpx;font-weight:600}.favorite__id{margin-top:8rpx;color:#8992a6;font-size:20rpx}
</style>
