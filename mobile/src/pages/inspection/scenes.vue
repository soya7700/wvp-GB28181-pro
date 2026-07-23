<template>
  <view class="page">
    <button class="preset" @click="createPreset">添加餐饮后厨预置模板</button>
    <view v-if="!items.length" class="empty">暂无场景模板</view>
    <view v-for="item in items" :key="item.id" class="card" @click="toggle(item.id)">
      <view class="head"><text class="title">{{ item.name }}</text><text class="badge">{{ item.status }}</text></view>
      <text class="desc">{{ item.description || '自定义场景模板' }}</text>
      <view v-if="expanded === item.id" class="regions">
        <view v-for="region in regions" :key="region.id" class="region">
          <text>{{ region.name }}</text><small>{{ region.algorithmCodes || '未配置算法' }}</small>
        </view>
      </view>
    </view>
  </view>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { createFoodServicePreset, querySceneRegions, querySceneTemplates, type SceneRegion, type SceneTemplate } from '@/api/inspection'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore(), items = ref<SceneTemplate[]>([]), regions = ref<SceneRegion[]>([]), expanded = ref(0)
async function load() { items.value = await querySceneTemplates() }
async function createPreset() { await createFoodServicePreset(); await load() }
async function toggle(id: number) { expanded.value = expanded.value === id ? 0 : id; regions.value = expanded.value ? await querySceneRegions(id) : [] }
onShow(() => { if (auth.requireAuth()) load() })
</script>
<style scoped>
.page{min-height:100vh;padding:24rpx;background:#f4f6fa;box-sizing:border-box}.preset{margin-bottom:20rpx;color:#fff;background:#175cd3}.card{margin-bottom:16rpx;padding:28rpx;background:#fff;border-radius:22rpx}.head{display:flex;justify-content:space-between}.title{font-size:29rpx;font-weight:650}.badge{font-size:20rpx;color:#475467}.desc{display:block;margin-top:12rpx;color:#667085;font-size:23rpx}.regions{margin-top:20rpx;border-top:1rpx solid #eaecf0}.region{padding:18rpx 0;border-bottom:1rpx solid #f2f4f7}.region text,.region small{display:block}.region small{margin-top:8rpx;color:#98a2b3;font-size:20rpx;word-break:break-all}.empty{padding:100rpx;text-align:center;color:#98a2b3}
</style>
