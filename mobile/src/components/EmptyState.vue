<template><view class="empty"><text class="icon">◎</text><text class="title">{{ title }}</text><text v-if="description" class="description">{{ description }}</text></view></template>
<script setup lang="ts">defineProps<{ title: string; description?: string }>()</script>
<style scoped>
.empty { padding: 100rpx 32rpx; display: flex; align-items: center; flex-direction: column; color: #8992a6; }
.icon { font-size: 72rpx; color: #c5ccda; }.title { margin-top: 20rpx; font-size: 30rpx; }.description { margin-top: 10rpx; font-size: 24rpx; }
</style>
