<template>
  <div class="app-container">
    <div class="head"><h2>AI 巡检管理</h2><el-button icon="el-icon-refresh" @click="load">刷新</el-button></div>
    <el-tabs v-model="tab" @tab-click="load">
      <el-tab-pane label="巡检计划" name="plans"><el-table :data="plans" border><el-table-column prop="name" label="计划名称" /><el-table-column prop="intervalMinutes" label="间隔(分钟)" width="110" /><el-table-column prop="detectionTypes" label="检测能力" /><el-table-column label="状态" width="90"><template slot-scope="{row}"><el-tag :type="row.enabled?'success':'info'">{{ row.enabled?'启用':'停用' }}</el-tag></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="执行任务" name="tasks"><el-table :data="tasks" border><el-table-column prop="planName" label="计划" /><el-table-column prop="status" label="状态" /><el-table-column prop="channelTotal" label="通道" /><el-table-column prop="abnormalCount" label="异常" /><el-table-column prop="startTime" label="开始时间" /></el-table></el-tab-pane>
      <el-tab-pane label="异常复核" name="results"><el-table :data="results" border><el-table-column prop="channelId" label="通道" /><el-table-column prop="detectionType" label="类型" /><el-table-column prop="confidence" label="置信度" /><el-table-column prop="status" label="状态" /><el-table-column label="操作" width="190"><template slot-scope="{row}"><el-button v-if="row.status==='PENDING'" size="mini" type="danger" @click="review(row,'CONFIRMED')">确认异常</el-button><el-button v-if="row.status==='PENDING'" size="mini" @click="review(row,'FALSE_POSITIVE')">误报</el-button></template></el-table-column></el-table></el-tab-pane>
      <el-tab-pane label="运维工单" name="orders"><el-table :data="orders" border><el-table-column prop="title" label="工单" /><el-table-column prop="priority" label="优先级" /><el-table-column prop="status" label="状态" /><el-table-column prop="dueTime" label="截止时间" /><el-table-column label="操作" width="180"><template slot-scope="{row}"><el-button v-if="row.status==='OPEN'" size="mini" @click="accept(row)">接单</el-button><el-button v-if="row.status==='RESOLVED'" size="mini" type="success" @click="verify(row,true)">验收</el-button><el-button v-if="row.status==='RESOLVED'" size="mini" type="danger" @click="verify(row,false)">退回</el-button></template></el-table-column></el-table></el-tab-pane>
    </el-tabs>
  </div>
</template>
<script>
import { inspectionPlans, inspectionTasks, inspectionResults, inspectionWorkOrders, reviewInspectionResult, acceptWorkOrder, verifyWorkOrder } from '@/api/inspection'
const list = v => (v && v.data && (v.data.list || v.data)) || (v && (v.list || v.items || v.data)) || []
export default { name:'AiInspectionManagement', data:()=>({tab:'plans',plans:[],tasks:[],results:[],orders:[]}), created(){this.load()}, methods:{
 async load(){ if(this.tab==='plans')this.plans=list(await inspectionPlans({page:1,count:100})); if(this.tab==='tasks')this.tasks=list(await inspectionTasks({page:1,count:100})); if(this.tab==='results')this.results=list(await inspectionResults({page:1,count:100})); if(this.tab==='orders')this.orders=list(await inspectionWorkOrders()) },
 async review(row,status){await reviewInspectionResult(row.id,status);this.load()}, async accept(row){await acceptWorkOrder(row.id);this.load()}, async verify(row,passed){await verifyWorkOrder(row.id,passed);this.load()}
}}</script>
<style scoped>.head{display:flex;justify-content:space-between;align-items:center}.head h2{margin-top:0}</style>
