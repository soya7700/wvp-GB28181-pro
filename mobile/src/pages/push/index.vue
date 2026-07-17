<template>
  <view class="page">
    <PageScroller :loading="loading" @refresh="reload" @load-more="load()">
      <SearchBar v-model="query" placeholder="推流名称、应用或流 ID" @search="reload" />
      <view class="list">
        <view v-for="item in items" :key="item.id" class="card">
          <view class="card__top">
            <text class="card__title">{{ item.name || item.stream || '未命名推流' }}</text>
            <StatusBadge :value="item.pushing ?? item.status" />
          </view>
          <text class="card__meta">应用：{{ item.app || '-' }}\n流 ID：{{ item.stream || '-' }}</text>
        </view>
        <EmptyState v-if="!loading && !items.length" title="暂无推流" />
        <view v-if="items.length" class="footer">
          {{ loading ? '加载中…' : finished ? '没有更多了' : '上拉加载更多' }}
        </view>
      </view>
    </PageScroller>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PageScroller from '@/components/PageScroller.vue'
import SearchBar from '@/components/SearchBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { queryPushes, type StreamItem } from '@/api/stream'
import { showRequestError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const query = ref('')
const items = ref<StreamItem[]>([])
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
    const result = await queryPushes({
      page: page.value,
      count: 20,
      query: query.value,
      pushing: '',
      mediaServerId: ''
    })
    const list = result.list || result.items || []
    items.value = reset ? list : [...items.value, ...list]
    finished.value = list.length < 20
    page.value++
  } catch (error) {
    showRequestError(error)
  } finally {
    loading.value = false
  }
}

function reload() {
  load(true)
}

onShow(() => {
  if (auth.requireAuth() && !items.value.length) load(true)
})
</script>

<style lang="scss" scoped>
@use '@/styles/list.scss';

.page {
  height: 100%;
  overflow: hidden;
}
</style>
