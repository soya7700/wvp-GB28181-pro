<template>
  <view class="page">
    <view class="brand"><view class="logo">W</view><text class="title">WVP Mobile</text><text class="subtitle">视频监控管理平台</text></view>
    <view class="form">
      <text class="label">用户名</text><input v-model="username" class="input" placeholder="请输入用户名" />
      <text class="label">密码</text><input v-model="password" class="input" password placeholder="请输入密码" @confirm="submit" />
      <button class="submit" :loading="loading" :disabled="loading" @click="submit">登录</button>
      <text class="hint">H5 调试默认连接本机 18080 端口</text>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { showRequestError } from '@/utils/request'
const auth = useAuthStore(); const username = ref(''); const password = ref(''); const loading = ref(false)
onShow(() => { auth.restore(); if (auth.loggedIn) uni.switchTab({ url: '/pages/dashboard/index' }) })
async function submit() {
  if (!username.value.trim() || !password.value) return uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
  loading.value = true
  try { await auth.signIn(username.value, password.value); uni.switchTab({ url: '/pages/dashboard/index' }) } catch (e) { showRequestError(e) } finally { loading.value = false }
}
</script>
<style scoped>
.page { min-height: 100vh; padding: calc(var(--status-bar-height) + 100rpx) 48rpx 48rpx; box-sizing: border-box; background: linear-gradient(155deg, #eef4ff 0%, #fff 48%, #f4f6fa 100%); }.brand { display: flex; align-items: center; flex-direction: column; }.logo { width: 112rpx; height: 112rpx; color: #fff; background: #2368f2; border-radius: 32rpx; text-align: center; line-height: 112rpx; font-size: 60rpx; font-weight: 700; box-shadow: 0 20rpx 44rpx rgba(35,104,242,.3); }.title { margin-top: 30rpx; font-size: 44rpx; font-weight: 700; }.subtitle { margin-top: 10rpx; color: #7b8498; font-size: 26rpx; }.form { margin-top: 80rpx; padding: 40rpx; background: rgba(255,255,255,.92); border-radius: 30rpx; box-shadow: 0 20rpx 70rpx rgba(40,59,91,.09); }.label { display: block; margin: 22rpx 0 12rpx; color: #515b70; font-size: 25rpx; }.input { height: 88rpx; padding: 0 26rpx; background: #f3f5f9; border-radius: 18rpx; font-size: 29rpx; }.submit { margin-top: 46rpx; height: 90rpx; color: #fff; background: #2368f2; border-radius: 20rpx; line-height: 90rpx; font-size: 30rpx; }.hint { display: block; margin-top: 24rpx; color: #a0a8b8; text-align: center; font-size: 22rpx; }
</style>
