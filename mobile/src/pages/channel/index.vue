<template>
  <PageScroller :loading="loading" @refresh="reload" @load-more="load">
    <SearchBar v-model="query" placeholder="通道名称或编号" @search="reload" />
    <view class="list">
      <view v-for="item in items" :key="item.id || item.deviceId" class="card" hover-class="card--pressed" @click="play(item)">
        <view class="card__top">
          <text class="card__title">{{ item.name || item.deviceId || '未命名通道' }}</text>
          <StatusBadge :value="item.status" />
        </view>
        <text class="card__meta">通道：{{ item.deviceId || '-' }}\n{{ item.ptzTypeText ? `云台：${item.ptzTypeText}\n` : '' }}点击进入实时预览</text>
      </view>
      <EmptyState v-if="!loading && !items.length" title="暂无通道" />
      <view v-if="items.length" class="footer">{{ loading ? '加载中…' : finished ? '没有更多了' : '上拉加载更多' }}</view>
    </view>
  </PageScroller>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import SearchBar from '@/components/SearchBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import PageScroller from '@/components/PageScroller.vue'
import { queryChannels, type Channel } from '@/api/device'
import { showRequestError } from '@/utils/request'

const parentDeviceId = ref('')
const query = ref('')
const items = ref<Channel[]>([])
const page = ref(1)
const loading = ref(false)
const finished = ref(false)

async function load(reset = false) {
  if (!parentDeviceId.value || loading.value || (!reset && finished.value)) return
  if (reset) {
    page.value = 1
    finished.value = false
  }
  loading.value = true
  try {
    const result = await queryChannels(parentDeviceId.value, {
      page: page.value,
      count: 20,
      query: query.value,
    })
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

function reload() {
  load(true)
}

function play(item: Channel) {
  const channelId = item.deviceId
  if (!channelId) {
    uni.showToast({ title: '该通道缺少国标编号，无法预览', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/pages/player/index?deviceId=${encodeURIComponent(parentDeviceId.value)}&channelId=${encodeURIComponent(channelId)}&name=${encodeURIComponent(item.name || channelId)}`,
    fail: (error) => uni.showToast({ title: error.errMsg || '无法打开预览页面', icon: 'none' }),
  })
}

onLoad((options) => {
  parentDeviceId.value = String(options?.deviceId || '')
  if (options?.name) uni.setNavigationBarTitle({ title: String(options.name) })
  load(true)
})
</script>

<style lang="scss" scoped>
@use '@/styles/list.scss';
.card--pressed { opacity: .72; transform: scale(.99); }
</style>
