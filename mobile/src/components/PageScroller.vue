<template>
  <scroll-view
    class="page-scroller"
    scroll-y
    :refresher-enabled="refreshable"
    :refresher-triggered="refreshing"
    :lower-threshold="lowerThreshold"
    :show-scrollbar="false"
    @refresherrefresh="onRefresh"
    @scrolltolower="$emit('load-more')"
  >
    <slot />
  </scroll-view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{ loading?: boolean; refreshable?: boolean; lowerThreshold?: number }>(), {
  loading: false,
  refreshable: true,
  lowerThreshold: 120,
})
const emit = defineEmits<{ (event: 'refresh'): void; (event: 'load-more'): void }>()
const refreshing = ref(false)

function onRefresh() {
  if (props.loading) return
  refreshing.value = true
  emit('refresh')
}

watch(() => props.loading, (loading) => {
  if (!loading) refreshing.value = false
})
</script>

<style scoped>
.page-scroller {
  width: 100%;
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  background: #f4f6fa;
}
</style>
