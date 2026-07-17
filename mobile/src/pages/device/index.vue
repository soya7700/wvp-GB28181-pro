<template>
  <view class="page">
    <PageScroller :loading="loading" @refresh="refresh" @load-more="loadMore">
      <view v-if="monitor.favorites.length || monitor.recents.length" class="shortcuts">
        <view class="shortcut-tabs">
          <text :class="shortcutTab === 'favorites' ? 'selected' : ''" @click="shortcutTab = 'favorites'">收藏 {{ monitor.favorites.length }}</text>
          <text :class="shortcutTab === 'recents' ? 'selected' : ''" @click="shortcutTab = 'recents'">最近观看</text>
        </view>
        <scroll-view class="shortcut-scroll" scroll-x :show-scrollbar="false">
          <view class="shortcut-row">
            <view v-for="item in shortcuts" :key="item.deviceId + item.channelId" class="shortcut" @click="playShortcut(item)">
              <text class="shortcut-name">{{ item.name }}</text>
              <text class="shortcut-id">{{ item.channelId }}</text>
            </view>
            <text v-if="!shortcuts.length" class="shortcut-empty">暂无记录</text>
          </view>
        </scroll-view>
      </view>

      <SearchBar v-model="query" placeholder="设备名称、编号或地址" @search="reload" />

      <view class="list">
        <view v-for="item in items" :key="item.deviceId" class="card" hover-class="card--pressed" @click="open(item)">
          <view class="card__top">
            <text class="card__title">{{ item.name || item.deviceId }}</text>
            <StatusBadge :value="item.status" />
          </view>
          <text class="card__meta">编号：{{ item.deviceId }}\n地址：{{ item.ip || '-' }}{{ item.port ? ':' + item.port : '' }}\n通道：{{ item.channelCount ?? '-' }}</text>
        </view>
        <EmptyState v-if="!loading && !items.length" title="暂无设备" description="请检查筛选条件或后端连接" />
        <view v-if="items.length" class="footer">{{ loading ? '加载中…' : finished ? '没有更多了' : '继续上滑加载更多' }}</view>
      </view>
    </PageScroller>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import SearchBar from '@/components/SearchBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PageScroller from '@/components/PageScroller.vue'
import { queryDevices, type Device } from '@/api/device'
import { showRequestError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { useMonitorStore, type ChannelShortcut } from '@/stores/monitor'

const auth = useAuthStore()
const monitor = useMonitorStore()
const shortcutTab = ref<'favorites' | 'recents'>('favorites')
const shortcuts = computed(() => shortcutTab.value === 'favorites' ? monitor.favorites : monitor.recents)
const query = ref('')
const items = ref<Device[]>([])
const page = ref(1)
const loading = ref(false)
const finished = ref(false)

async function load(reset = false) {
  if (loading.value || (!reset && finished.value)) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const result = await queryDevices({ page: page.value, count: 20, query: query.value })
    const list = result.list || result.items || []
    items.value = reset ? list : [...items.value, ...list]
    finished.value = list.length < 20
    page.value += 1
  } catch (error) {
    showRequestError(error)
  } finally {
    loading.value = false
  }
}

async function refresh() {
  await load(true)
}

function reload() {
  load(true)
}

function loadMore() {
  load()
}

function open(item: Device) {
  uni.navigateTo({ url: `/pages/channel/index?deviceId=${encodeURIComponent(item.deviceId)}&name=${encodeURIComponent(item.name || item.deviceId)}` })
}

function playShortcut(item: ChannelShortcut) {
  uni.navigateTo({ url: `/pages/player/index?deviceId=${encodeURIComponent(item.deviceId)}&channelId=${encodeURIComponent(item.channelId)}&name=${encodeURIComponent(item.name)}` })
}

onShow(() => {
  monitor.restore()
  if (auth.requireAuth() && !items.value.length) load(true)
})
</script>

<style lang="scss" scoped>
@use '@/styles/list.scss';

.page { height: 100%; min-height: 0; overflow: hidden; }
.shortcuts { padding: 20rpx 24rpx; background: #fff; }
.shortcut-tabs { display: flex; gap: 34rpx; margin-bottom: 20rpx; color: #8992a6; font-size: 26rpx; }
.shortcut-tabs .selected { color: #2368f2; font-weight: 600; }
.shortcut-scroll { width: 100%; }
.shortcut-row { display: flex; gap: 14rpx; width: max-content; min-width: 100%; }
.shortcut { width: 240rpx; flex: none; padding: 18rpx 20rpx; background: #f3f6fb; border-radius: 18rpx; box-sizing: border-box; }
.shortcut-name, .shortcut-id { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.shortcut-name { font-size: 25rpx; font-weight: 600; }
.shortcut-id { margin-top: 8rpx; color: #8992a6; font-size: 20rpx; }
.shortcut-empty { padding: 12rpx; color: #a0a8b8; font-size: 23rpx; }
.card--pressed { opacity: .72; transform: scale(.99); }
</style>
