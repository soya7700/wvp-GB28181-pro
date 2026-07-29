<template><text class="badge" :class="online ? 'online' : 'offline'">{{ online ? '在线' : '离线' }}</text></template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ value?: boolean | string | number }>()
const online = computed(() => props.value === true || props.value === 1 || props.value === '1' || props.value === 'ON' || props.value === 'ONLINE')
</script>
<style scoped>.badge { padding: 6rpx 14rpx; border-radius: 999rpx; font-size: 22rpx; }.online { color: #14804a; background: #e4f7ed; }.offline { color: #7b8498; background: #edf0f5; }</style>
