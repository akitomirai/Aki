<template>
  <div class="feedback-manage">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>消费者反馈管理</span>
          <el-button type="primary" link @click="loadFeedbackList">刷新</el-button>
        </div>
      </template>

      <el-table :data="feedbackList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="batchId" label="批次ID" width="100" />
        <el-table-column prop="qrId" label="二维码ID" width="100" />
        <el-table-column prop="feedbackType" label="反馈类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ getFeedbackTypeText(row.feedbackType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="电话" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetailDialog(row)">查看</el-button>
            <el-button v-if="canHandle" type="primary" link @click="openHandleDialog(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="反馈详情"
      width="640px"
      append-to-body
    >
      <div v-loading="detailLoading">
        <el-descriptions v-if="currentDetail" :column="2" border>
          <el-descriptions-item label="ID">{{ currentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="批次ID">{{ currentDetail.batchId }}</el-descriptions-item>
          <el-descriptions-item label="二维码ID">{{ currentDetail.qrId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ getFeedbackTypeText(currentDetail.feedbackType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getStatusText(currentDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentDetail.sourceChannel }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentDetail.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentDetail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">{{ currentDetail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="反馈内容" :span="2">{{ currentDetail.content }}</el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2">{{ currentDetail.handleResult || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处理反馈对话框 -->
    <el-dialog
      v-model="handleVisible"
      title="反馈处理"
      width="500px"
      append-to-body
    >
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="反馈内容">
          <div class="feedback-content-preview">
            {{ feedbackList.find(f => f.id === handleForm.id)?.content }}
          </div>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="handleForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="CLOSED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input
            v-model="handleForm.handleResult"
            type="textarea"
            :rows="4"
            placeholder="请输入处理结果或答复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleVisible = false">取消</el-button>
          <el-button type="primary" :loading="handleLoading" @click="submitHandle">
            提交处理
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useFeedbackManage } from '../../composables/useFeedbackManage'

const {
  canHandle,
  loading,
  feedbackList,
  handleVisible,
  handleLoading,
  detailVisible,
  detailLoading,
  currentDetail,
  handleForm,
  loadFeedbackList,
  openDetailDialog,
  openHandleDialog,
  submitHandle,
  getFeedbackTypeText,
  getStatusText,
  getStatusTagType
} = useFeedbackManage()
</script>

<style scoped>
.feedback-manage {
  padding: 20px;
}
.table-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.feedback-content-preview {
  padding: 8px 12px;
  background-color: #f8fafc;
  border-radius: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  width: 100%;
}
</style>
