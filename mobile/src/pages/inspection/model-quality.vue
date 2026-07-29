<template>
  <view class="page">
    <view v-for="item in metrics" :key="item.modelId" class="card">
      <view class="head"><text class="title">{{ item.modelName }} {{ item.modelVersion }}</text><text class="badge" :class="item.qualityStatus.toLowerCase()">{{ qualityText[item.qualityStatus] }}</text></view>
      <view class="rates"><view><b>{{ pct(item.confirmationRate) }}</b><small>确认率</small></view><view><b>{{ pct(item.falsePositiveRate) }}</b><small>误报率</small></view><view><b>{{ item.totalCount }}</b><small>样本</small></view></view>
      <view class="rollout"><text>灰度流量 {{ traffic(item.modelId) }}%</text><button v-for="percent in [10,50,100]" :key="percent" size="mini" @click="setTraffic(item.modelId, percent)">{{ percent }}%</button></view>
    </view>
    <view v-if="!metrics.length" class="empty">暂无模型质量数据</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { queryAiModels, queryModelQuality, rolloutAiModel, type AiModel, type ModelQuality } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const metrics = ref<ModelQuality[]>([])
const models = ref<AiModel[]>([])
const qualityText = { INSUFFICIENT_DATA: '样本不足', DRIFT_RISK: '漂移风险', STABLE: '稳定' }
const pct = (value: number) => `${Math.round(value * 100)}%`
const traffic = (id: number) => models.value.find(item => item.id === id)?.trafficPercent || 0
async function load() { [metrics.value, models.value] = await Promise.all([queryModelQuality(), queryAiModels()]) }
async function setTraffic(id: number, percent: number) { await rolloutAiModel(id, percent); await load() }
onShow(() => { if (auth.requireAuth()) load() })
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;align-items:center;justify-content:space-between;gap:16rpx}.title{font-size:28rpx;font-weight:650}.badge{padding:7rpx 13rpx;border-radius:99rpx;font-size:20rpx;color:#475467;background:#f2f4f7}.badge.stable{color:#027a48;background:#ecfdf3}.badge.drift_risk{color:#b42318;background:#fef3f2}.rates{display:grid;grid-template-columns:repeat(3,1fr);margin-top:24rpx;text-align:center}.rates b,.rates small{display:block}.rates b{font-size:32rpx}.rates small{margin-top:6rpx;color:#98a2b3;font-size:20rpx}.rollout{display:flex;align-items:center;gap:10rpx;margin-top:24rpx;padding-top:20rpx;border-top:1rpx solid #edf0f5}.rollout text{flex:1;color:#667085;font-size:22rpx}.rollout button{margin:0;color:#175cd3}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
