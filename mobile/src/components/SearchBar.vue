<template><view class="search"><input v-model="value" class="input" confirm-type="search" :placeholder="placeholder" @confirm="$emit('search', value)" /><button class="button" size="mini" @click="$emit('search', value)">搜索</button></view></template>
<script setup lang="ts">
import { ref, watch } from 'vue'
const props = withDefaults(defineProps<{ modelValue?: string; placeholder?: string }>(), { modelValue: '', placeholder: '搜索' })
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void; (e: 'search', value: string): void }>()
const value = ref(props.modelValue)
watch(value, (next) => emit('update:modelValue', next))
</script>
<style scoped>
.search { display: flex; gap: 16rpx; padding: 20rpx 24rpx; background: #fff; }.input { flex: 1; height: 72rpx; padding: 0 24rpx; border-radius: 18rpx; background: #f2f4f8; font-size: 28rpx; }.button { width: 128rpx; height: 72rpx; line-height: 72rpx; margin: 0; color: #fff; background: #2368f2; border-radius: 18rpx; font-size: 27rpx; }
</style>
