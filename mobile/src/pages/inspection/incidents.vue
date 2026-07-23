<template>
  <view class="page">
    <view v-if="!items.length" class="empty">暂无未恢复的聚合事件</view>
    <view v-for="item in items" :key="item.aggregationKey" class="card">
      <view class="head"><text class="cause">{{ causeText(item.rootCause) }}</text><text class="count">{{ item.eventCount }} 次</text></view>
      <text class="key">{{ item.aggregationKey }}</text>
      <view class="meta"><text>影响 {{ item.affectedChannels }} 个通道</text><text>{{ item.latestTime }}</text></view>
      <button size="mini" @click="recover(item.aggregationKey)">确认恢复</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { queryIncidentGroups, recoverIncident, type IncidentGroup } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const items = ref<IncidentGroup[]>([])
const names: Record<string, string> = { DEVICE_OR_NETWORK: '设备或网络异常', MEDIA_OR_NETWORK: '媒体或网络异常', STORAGE: '存储异常', AI_SERVICE: 'AI 服务异常', CAMERA_OR_SCENE: '摄像机或场景异常', UNKNOWN: '待分析异常' }
const causeText = (cause: string) => names[cause] || cause
async function load() { items.value = await queryIncidentGroups() }
async function recover(key: string) { await recoverIncident(key); await load() }
onShow(() => { if (auth.requireAuth()) load() })
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head,.meta{display:flex;align-items:center;justify-content:space-between}.cause{font-size:29rpx;font-weight:650}.count{padding:7rpx 14rpx;color:#b54708;background:#fffaeb;border-radius:99rpx;font-size:21rpx}.key{display:block;margin-top:14rpx;color:#667085;font-size:22rpx}.meta{margin-top:18rpx;color:#98a2b3;font-size:21rpx}.card button{margin:20rpx 0 0 auto;color:#175cd3}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
