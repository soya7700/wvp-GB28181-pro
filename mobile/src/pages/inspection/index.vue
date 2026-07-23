<template>
  <view class="page">
    <view class="hero">
      <view>
        <text class="hero__title">AI 视频巡检</text>
        <text class="hero__desc">从视频质量检测开始，逐步形成异常处置闭环</text>
      </view>
      <text class="service" :class="overview?.enabled ? 'ready' : ''">{{ serviceText }}</text>
    </view>

    <view class="summary">
      <view class="metric"><text class="metric__value">{{ availableCount }}</text><text class="metric__label">已具备能力</text></view>
      <view class="metric"><text class="metric__value">{{ waitingCount }}</text><text class="metric__label">待接入能力</text></view>
      <view class="metric"><text class="metric__value">{{ overview?.enabled ? '已开启' : '未开启' }}</text><text class="metric__label">AI 服务</text></view>
    </view>

    <view class="actions">
      <view class="action" @click="open('/pages/inspection/plans')"><text class="action__icon">✓</text><text>巡检计划</text></view>
      <view class="action" @click="open('/pages/inspection/results')"><text class="action__icon warning">!</text><text>巡检异常</text></view>
      <view class="action" @click="open('/pages/inspection/report')"><text class="action__icon report">≡</text><text>巡检报告</text></view>
      <view class="action" @click="open('/pages/inspection/models')"><text class="action__icon model">◎</text><text>模型与规则</text></view>
      <view class="action" @click="open('/pages/inspection/health')"><text class="action__icon health">+</text><text>视频健康</text></view>
      <view class="action" @click="open('/pages/inspection/work-orders')"><text class="action__icon order">↗</text><text>运维工单</text></view>
      <view class="action" @click="open('/pages/inspection/incidents')"><text class="action__icon incident">∑</text><text>聚合事件</text></view>
      <view class="action" @click="open('/pages/inspection/model-quality')"><text class="action__icon quality">%</text><text>模型质量</text></view>
      <view class="action" @click="open('/pages/inspection/scenes')"><text class="action__icon scene">▦</text><text>场景模板</text></view>
    </view>

    <view class="section">
      <view class="section__head"><text>能力准备情况</text><text class="refresh" @click="load">刷新</text></view>
      <view v-if="loading" class="state">正在检查服务能力…</view>
      <view v-else-if="error" class="state error"><text>{{ error }}</text><button size="mini" @click="load">重新检查</button></view>
      <view v-else class="capabilities">
        <view v-for="item in overview?.capabilities" :key="item.code" class="capability">
          <view class="capability__main"><text class="capability__name">{{ item.name }}</text><text class="capability__desc">{{ item.description }}</text></view>
          <text class="badge" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</text>
        </view>
      </view>
    </view>

    <view class="section roadmap">
      <text class="section__title">首期巡检范围</text>
      <view class="roadmap__item"><text class="step">1</text><text>定时抓取通道画面</text></view>
      <view class="roadmap__item"><text class="step">2</text><text>检测黑屏、冻结、模糊和遮挡</text></view>
      <view class="roadmap__item"><text class="step">3</text><text>异常生成告警与站内信</text></view>
      <view class="roadmap__item"><text class="step">4</text><text>移动端人工复核和闭环处理</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getInspectionOverview, type InspectionCapabilityStatus, type InspectionOverview } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const overview = ref<InspectionOverview>()
const loading = ref(false)
const error = ref('')
const availableCount = computed(() => overview.value?.capabilities.filter(item => item.status === 'AVAILABLE').length || 0)
const waitingCount = computed(() => overview.value?.capabilities.filter(item => item.status !== 'AVAILABLE').length || 0)
const serviceText = computed(() => overview.value?.enabled ? '服务已开启' : '待配置')

function statusText(status: InspectionCapabilityStatus) {
  return { AVAILABLE: '可复用', PLANNED: '待实现', WAITING: '待接入' }[status]
}

function open(url: string) {
  uni.navigateTo({ url })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    overview.value = await getInspectionOverview()
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '暂时无法获取巡检状态'
  } finally {
    loading.value = false
  }
}

onShow(() => {
  if (auth.requireAuth()) load()
})
</script>

<style scoped>
.page{min-height:100vh;padding:24rpx;box-sizing:border-box}.hero{display:flex;align-items:flex-start;justify-content:space-between;gap:20rpx;padding:36rpx 32rpx;color:#fff;background:linear-gradient(135deg,#172033,#315487);border-radius:28rpx}.hero__title,.hero__desc{display:block}.hero__title{font-size:38rpx;font-weight:700}.hero__desc{max-width:480rpx;margin-top:14rpx;opacity:.76;font-size:23rpx;line-height:1.55}.service{flex:none;padding:8rpx 16rpx;color:#d0d5dd;background:rgba(255,255,255,.12);border-radius:999rpx;font-size:21rpx}.service.ready{color:#b7f4d0;background:rgba(27,158,89,.24)}.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:14rpx;margin-top:20rpx}.metric{padding:24rpx 8rpx;text-align:center;background:#fff;border-radius:20rpx}.metric__value,.metric__label{display:block}.metric__value{font-size:30rpx;font-weight:700}.metric__label{margin-top:8rpx;color:#8992a6;font-size:21rpx}.actions{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;margin-top:18rpx}.action{display:flex;align-items:center;gap:18rpx;padding:24rpx;background:#fff;border-radius:20rpx;font-size:26rpx;font-weight:600}.action__icon{width:48rpx;height:48rpx;color:#14804a;background:#e4f7ed;border-radius:14rpx;text-align:center;line-height:48rpx}.action__icon.warning{color:#b54708;background:#ffead5}.action__icon.report{color:#2368f2;background:#e9f0ff}.action__icon.model{color:#6941c6;background:#f1ebff}.section{margin-top:22rpx;padding:28rpx;background:#fff;border-radius:24rpx}.section__head{display:flex;justify-content:space-between;font-size:29rpx;font-weight:600}.refresh{color:#2368f2;font-size:24rpx;font-weight:400}.state{padding:60rpx 0;text-align:center;color:#8992a6;font-size:24rpx}.state button{margin-top:18rpx;color:#2368f2}.error{color:#d84b4b}.capability{display:flex;align-items:center;gap:18rpx;padding:24rpx 0;border-bottom:1rpx solid #edf0f5}.capability:last-child{border-bottom:0}.capability__main{min-width:0;flex:1}.capability__name,.capability__desc{display:block}.capability__name{font-size:27rpx;font-weight:600}.capability__desc{margin-top:8rpx;color:#8992a6;font-size:22rpx}.badge{padding:7rpx 14rpx;color:#667085;background:#edf0f5;border-radius:999rpx;font-size:20rpx}.badge.available{color:#14804a;background:#e4f7ed}.badge.planned{color:#2368f2;background:#e9f0ff}.section__title{font-size:29rpx;font-weight:600}.roadmap__item{display:flex;align-items:center;gap:18rpx;margin-top:24rpx;color:#475467;font-size:25rpx}.step{width:42rpx;height:42rpx;color:#2368f2;background:#e9f0ff;border-radius:50%;text-align:center;line-height:42rpx;font-weight:600}
.action__icon.health{color:#027a48;background:#ecfdf3}
.action__icon.order{color:#c4320a;background:#fff1eb}
.action__icon.incident{color:#5925dc;background:#f4f3ff}
.action__icon.quality{color:#026aa2;background:#f0f9ff}
.action__icon.scene{color:#3538cd;background:#eef4ff}
</style>
