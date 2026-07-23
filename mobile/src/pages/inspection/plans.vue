<template>
  <view class="page">
    <PageScroller :loading="loading" @refresh="load">
      <view class="toolbar"><button size="mini" class="primary" @click="openForm()">新增计划</button></view>
      <view v-if="editing" class="form">
        <input v-model="form.name" placeholder="计划名称" />
        <input v-model="form.channelIds" placeholder="通道编号，多个用逗号分隔" />
        <input v-model="form.detectionTypes" placeholder="检测类型，多个用逗号分隔" />
        <view class="row"><input v-model.number="form.intervalMinutes" type="number" placeholder="间隔分钟" /><input v-model="form.scheduleDays" placeholder="星期 1,2,3..." /></view>
        <view class="row"><input v-model="form.startTime" placeholder="开始 08:00" /><input v-model="form.endTime" placeholder="结束 18:00" /></view>
        <view class="actions"><button size="mini" @click="editing=false">取消</button><button size="mini" class="primary" @click="save">保存</button></view>
      </view>
      <view class="list">
        <view v-for="item in items" :key="item.id" class="card">
          <view class="card__top"><text class="card__title">{{ item.name }}</text><text class="status" :class="item.enabled?'enabled':''">{{ item.enabled?'已启用':'已停用' }}</text></view>
          <text class="card__meta">周期：每 {{ item.intervalMinutes }} 分钟\n时间：周{{ item.scheduleDays }} {{ item.startTime }}-{{ item.endTime }}\n检测：{{ typeText(item.detectionTypes) }}\n通道：{{ channelCount(item.channelIds) }} 个</text>
          <view class="card-actions">
            <button size="mini" @click="run(item)">{{ running===item.id?'创建中':'立即巡检' }}</button>
            <button size="mini" @click="openForm(item)">编辑</button>
            <button size="mini" @click="toggle(item)">{{ item.enabled?'停用':'启用' }}</button>
            <button size="mini" @click="copy(item)">复制</button>
            <button size="mini" class="danger" @click="remove(item)">删除</button>
          </view>
        </view>
        <EmptyState v-if="!loading&&!items.length" title="暂无巡检计划" description="点击上方按钮创建计划" />
      </view>
    </PageScroller>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import PageScroller from '@/components/PageScroller.vue'
import EmptyState from '@/components/EmptyState.vue'
import { copyInspectionPlan, createInspectionPlan, deleteInspectionPlan, queryInspectionPlans, runInspectionPlan, toggleInspectionPlan, updateInspectionPlan, type InspectionPlan } from '@/api/inspection'
import { showRequestError } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const items = ref<InspectionPlan[]>([])
const loading = ref(false)
const running = ref<number>()
const editing = ref(false)
const form = reactive<InspectionPlan>({ id: 0, name: '', enabled: true, intervalMinutes: 30, detectionTypes: 'BLACK_SCREEN,FREEZE,BLUR,OCCLUSION', channelIds: '', scheduleDays: '1,2,3,4,5,6,7', startTime: '00:00', endTime: '23:59' })
async function load(){ if(loading.value)return; loading.value=true; try{const r=await queryInspectionPlans();items.value=r.list||r.items||[]}catch(e){showRequestError(e)}finally{loading.value=false} }
function openForm(item?:InspectionPlan){ Object.assign(form,item||{id:0,name:'',enabled:true,intervalMinutes:30,detectionTypes:'BLACK_SCREEN,FREEZE,BLUR,OCCLUSION',channelIds:'',scheduleDays:'1,2,3,4,5,6,7',startTime:'00:00',endTime:'23:59'}); editing.value=true }
async function save(){ try{const payload={...form};if(payload.id){await updateInspectionPlan(payload)}else{const{id,...newPlan}=payload;await createInspectionPlan(newPlan)}editing.value=false;await load()}catch(e){showRequestError(e)} }
async function run(item:InspectionPlan){running.value=item.id;try{await runInspectionPlan(item.id);uni.showToast({title:'任务已创建'})}catch(e){showRequestError(e)}finally{running.value=undefined}}
async function toggle(item:InspectionPlan){try{await toggleInspectionPlan(item.id,!item.enabled);await load()}catch(e){showRequestError(e)}}
async function copy(item:InspectionPlan){try{await copyInspectionPlan(item.id);await load()}catch(e){showRequestError(e)}}
async function remove(item:InspectionPlan){const result=await uni.showModal({title:'删除计划',content:`确认删除“${item.name}”？`});if(!result.confirm)return;try{await deleteInspectionPlan(item.id);await load()}catch(e){showRequestError(e)}}
function typeText(v:string){const names:Record<string,string>={BLACK_SCREEN:'黑屏',FREEZE:'冻结',BLUR:'模糊',OCCLUSION:'遮挡'};return v.split(',').map(i=>names[i]||i).join('、')}
function channelCount(v?:string){return v?v.split(',').filter(Boolean).length:0}
onShow(()=>{if(auth.requireAuth())load()})
</script>

<style lang="scss" scoped>
@use '@/styles/list.scss';
.page{height:100%;overflow:hidden}.toolbar{padding:20rpx 24rpx 0;display:flex;justify-content:flex-end}.form{margin:20rpx 24rpx 0;padding:24rpx;background:#fff;border-radius:20rpx}.form input{margin-bottom:16rpx;padding:18rpx;background:#f4f6fa;border-radius:12rpx}.row,.actions,.card-actions{display:flex;gap:12rpx}.row input{min-width:0;flex:1}.actions{justify-content:flex-end}.primary{color:#fff;background:#2368f2}.danger{color:#d92d20}.status{padding:7rpx 14rpx;color:#667085;background:#edf0f5;border-radius:999rpx;font-size:21rpx}.status.enabled{color:#14804a;background:#e4f7ed}.card-actions{margin-top:18rpx;flex-wrap:wrap}.card-actions button{margin:0}
</style>
