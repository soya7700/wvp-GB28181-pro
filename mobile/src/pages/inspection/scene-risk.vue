<template>
  <view class="page">
    <view v-if="!items.length" class="empty">暂无场景风险数据</view>
    <view v-for="item in items" :key="item.templateId" class="card">
      <view class="head"><text class="title">{{ item.templateName }}</text><text class="risk" :class="item.riskLevel.toLowerCase()">{{ riskName(item.riskLevel) }}</text></view>
      <text class="score">{{ item.riskScore }}</text><text class="label">风险积分</text>
      <view class="metrics"><text>事件 {{ item.eventCount }}</text><text>待处理 {{ item.openCount }}</text><text>高风险 {{ item.highRiskCount }}</text></view>
      <view class="metrics"><text>已确认 {{ item.confirmedCount }}</text><text>误报 {{ item.falsePositiveCount }}</text></view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { querySceneRisk, type SceneRiskSummary } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), items = ref<SceneRiskSummary[]>([])
function riskName(level: string) { return { LOW: '低', NORMAL: '一般', HIGH: '高', CRITICAL: '严重' }[level] || level }
onShow(async () => { if (auth.requireAuth()) items.value = await querySceneRisk() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.card{margin-bottom:18rpx;padding:30rpx;background:#fff;border-radius:24rpx}.head{display:flex;justify-content:space-between}.title{font-size:30rpx;font-weight:650}.risk{padding:6rpx 15rpx;color:#475467;background:#f2f4f7;border-radius:999rpx;font-size:20rpx}.risk.high,.risk.critical{color:#b42318;background:#fef3f2}.score{display:inline-block;margin-top:22rpx;font-size:48rpx;font-weight:700}.label{margin-left:12rpx;color:#98a2b3;font-size:21rpx}.metrics{display:flex;gap:25rpx;margin-top:18rpx;color:#667085;font-size:22rpx}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
