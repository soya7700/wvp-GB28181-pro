<template>
  <view class="page">
    <button class="preset" @click="createPreset">启用人员规范算法预置</button>
    <button class="preset environment" @click="createEnvironmentPreset">启用环境卫生算法预置</button>
    <view class="summary">已启用 {{ algorithms.length }} 项算法 · 最近 {{ events.length }} 条事件</view>
    <view v-if="!events.length" class="empty">暂无算法事件</view>
    <view v-for="event in events" :key="event.id" class="card">
      <view class="head">
        <text class="title">{{ algorithmName(event.algorithmCode) }}</text>
        <text class="badge" :class="event.state.toLowerCase()">{{ stateName(event.state) }}</text>
      </view>
      <text class="meta">通道 {{ event.channelId }} · 持续 {{ event.durationSeconds || 0 }} 秒</text>
      <text class="meta">置信度 {{ confidence(event.confidence) }} · {{ event.createTime }}</text>
      <button v-if="event.state === 'OPEN'" class="recover" size="mini" @click="recover(event.id)">标记已恢复</button>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createEnvironmentAlgorithms, createPersonnelAlgorithms, queryAlgorithmEvents, queryAlgorithms, recoverAlgorithmEvent, type AlgorithmDefinition, type AlgorithmEvent } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), algorithms = ref<AlgorithmDefinition[]>([]), events = ref<AlgorithmEvent[]>([])
async function load() { [algorithms.value, events.value] = await Promise.all([queryAlgorithms(), queryAlgorithmEvents()]) }
async function createPreset() { await createPersonnelAlgorithms(); await load() }
async function createEnvironmentPreset() { await createEnvironmentAlgorithms(); await load() }
async function recover(id: number) { await recoverAlgorithmEvent(id); await load() }
function algorithmName(code: string) { return algorithms.value.find(item => item.code === code)?.name || code }
function stateName(state: string) { return { OBSERVING: '观察中', OPEN: '待处理', SUPPRESSED: '已抑制', RECOVERED: '已恢复', CLOSED: '已关闭' }[state] || state }
function confidence(value?: number) { return value == null ? '-' : `${Math.round(value * 100)}%` }
onShow(() => { if (auth.requireAuth()) load() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.preset{color:#fff;background:#175cd3}.preset.environment{margin-top:12rpx;background:#027a48}.summary{padding:22rpx 4rpx;color:#667085;font-size:23rpx}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;justify-content:space-between;gap:16rpx}.title{font-size:29rpx;font-weight:650}.badge{padding:6rpx 14rpx;color:#b54708;background:#fffaeb;border-radius:999rpx;font-size:20rpx}.badge.open{color:#b42318;background:#fef3f2}.meta{display:block;margin-top:12rpx;color:#667085;font-size:22rpx}.recover{margin:18rpx 0 0;color:#027a48}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
