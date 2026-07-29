<template>
  <div class="app-container">
    <div class="page-head"><div><h2>智能巡检中心</h2><p>固定视频 AI 巡检与线下移动巡店统一管理</p></div><el-button icon="el-icon-refresh" @click="load">刷新</el-button></div>
    <el-alert v-if="health && !health.migrationReady" title="数据库结构尚未升级完成" type="error" show-icon :closable="false" />
    <el-row :gutter="16" class="metrics">
      <el-col v-for="item in cards" :key="item.label" :span="6"><el-card shadow="never"><div class="value">{{ item.value }}</div><div class="label">{{ item.label }}</div></el-card></el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="14"><el-card shadow="never"><div slot="header">能力准备情况</div>
        <el-table :data="overview.capabilities || []" size="small"><el-table-column prop="name" label="能力" /><el-table-column prop="description" label="说明" /><el-table-column label="状态" width="100"><template slot-scope="{row}"><el-tag :type="row.status==='AVAILABLE'?'success':'warning'">{{ statusName(row.status) }}</el-tag></template></el-table-column></el-table>
      </el-card></el-col>
      <el-col :span="10"><el-card shadow="never"><div slot="header">服务状态</div>
        <el-descriptions :column="1" border size="small"><el-descriptions-item label="数据库">{{ health && health.migrationReady ? '正常' : '待升级' }}</el-descriptions-item><el-descriptions-item label="AI 服务">{{ health && health.aiConfigured ? '已配置' : '未配置' }}</el-descriptions-item><el-descriptions-item label="阶段">{{ overview.phase || '-' }}</el-descriptions-item></el-descriptions>
      </el-card></el-col>
    </el-row>
  </div>
</template>
<script>
import { inspectionOverview, inspectionHealth, visitOperations } from '@/api/inspection'
export default { name: 'InspectionOverview', data: () => ({ overview: {}, health: null, operations: {} }),
  computed: { cards() { return [{ label: '巡店任务', value: this.operations.taskCount || 0 }, { label: '覆盖门店', value: this.operations.storeCount || 0 }, { label: '问题数量', value: this.operations.problemCount || 0 }, { label: '逾期整改', value: this.operations.overdueCount || 0 }] } },
  created() { this.load() }, methods: { async load() { const [a, b, c] = await Promise.all([inspectionOverview(), inspectionHealth(), visitOperations(30)]); this.overview=a.data||a; this.health=b.data||b; this.operations=c.data||c }, statusName(v) { return { AVAILABLE:'可用', PLANNED:'规划中', WAITING:'待接入' }[v]||v } } }
</script>
<style scoped>.page-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.page-head h2{margin:0 0 8px}.page-head p{margin:0;color:#909399}.metrics{margin:18px 0}.value{font-size:30px;font-weight:700}.label{margin-top:8px;color:#909399}</style>
