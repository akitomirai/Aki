<template>
  <el-row :gutter="20" class="quality-page">
    <el-col v-if="canWrite" :span="10">
      <el-card>
        <template #header>
          <span>上传质检报告文件</span>
        </template>

        <el-upload
            class="upload-box"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            :on-change="handleFileChange"
        >
          <template #trigger>
            <el-button type="primary" plain>选择文件</el-button>
          </template>
        </el-upload>

        <div class="upload-action">
          <el-button type="success" :loading="uploading" @click="handleUpload">
            上传文件
          </el-button>
        </div>

        <div v-if="uploadedUrl" class="result-box">
          <p><strong>上传成功：</strong></p>
          <p class="url-text">{{ uploadedUrl }}</p>
        </div>
      </el-card>
    </el-col>

    <el-col :span="canWrite ? 14 : 24">
      <el-card v-if="canWrite" style="margin-bottom: 16px;">
        <template #header>
          <span>新增质检报告（自动存证）</span>
        </template>

        <el-form :model="form" label-width="100px">
          <el-form-item label="批次ID">
            <el-input v-model="form.batchId" placeholder="请输入批次ID" />
          </el-form-item>

          <el-form-item label="报告编号">
            <el-input v-model="form.reportNo" placeholder="如：R-2026-001" />
          </el-form-item>

          <el-form-item label="检测机构">
            <el-input v-model="form.agency" placeholder="如：赣州某检测机构" />
          </el-form-item>

          <el-form-item label="检测结果">
            <el-select v-model="form.result" class="full-width">
              <el-option label="合格" value="合格" />
              <el-option label="不合格" value="不合格" />
            </el-select>
          </el-form-item>

          <el-form-item label="报告URL">
            <el-input v-model="form.reportFileUrl" placeholder="上传后自动填充，也可手动粘贴" />
          </el-form-item>

          <el-form-item label="扩展JSON">
            <div class="json-template-row">
              <el-select v-model="createTemplateKey" style="width: 220px" placeholder="选择示例模板">
                <el-option
                  v-for="item in jsonTemplateOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button @click="applyJsonTemplate('create', form, createTemplateKey)">应用模板</el-button>
            </div>
            <el-input
                v-model="form.reportJson"
                type="textarea"
                :rows="6"
                placeholder='例如：{"items":[{"name":"农残","value":"合格"}]}'
            />
            <div class="json-tool-row">
              <el-button link type="primary" @click="formatJsonInput('create', form)">格式化JSON</el-button>
              <el-button link @click="fillJsonExample('create', form)">填充示例</el-button>
              <el-button link type="danger" @click="clearJsonInput('create', form)">清空</el-button>
            </div>
            <div class="json-valid-row">
              <el-tag :type="isJsonValid(form.reportJson) ? 'success' : 'danger'" size="small">
                {{ isJsonValid(form.reportJson) ? 'JSON格式合法' : 'JSON格式有误' }}
              </el-tag>
              <span class="json-valid-tip">扩展JSON用于补充检测项，如农残、重金属、微生物指标。</span>
            </div>
            <div class="json-tip">
              提示：请输入合法 JSON，提交失败常见原因是缺少引号、逗号或括号未闭合。
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              提交质检报告
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card>
        <template #header>
          <div class="list-header">
            <span>质检报告查询</span>
            <div class="list-ops">
              <el-input v-model="queryBatchId" placeholder="输入批次ID" style="width: 200px;" />
              <el-button type="primary" :loading="listLoading" @click="loadReportList">查询</el-button>
            </div>
          </div>
        </template>

        <el-table :data="reportList" v-loading="listLoading" border stripe>
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="batchId" label="批次ID" width="120" />
          <el-table-column prop="reportNo" label="报告编号" min-width="160" />
          <el-table-column prop="agency" label="检测机构" min-width="160" />
          <el-table-column prop="result" label="结论" width="100" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id, 'view')">查看</el-button>
              <el-button v-if="canWrite" link type="primary" @click="openDetail(row.id, 'edit')">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="detailDialogVisible" :title="detailMode === 'edit' ? '编辑质检报告' : '质检报告详情'" width="720px">
    <div v-loading="detailLoading">
      <template v-if="currentReport">
        <el-descriptions v-if="detailMode === 'view'" :column="2" border>
          <el-descriptions-item label="ID">{{ currentReport.id }}</el-descriptions-item>
          <el-descriptions-item label="批次ID">{{ currentReport.batchId }}</el-descriptions-item>
          <el-descriptions-item label="报告编号">{{ currentReport.reportNo }}</el-descriptions-item>
          <el-descriptions-item label="检测机构">{{ currentReport.agency }}</el-descriptions-item>
          <el-descriptions-item label="结论">{{ currentReport.result }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentReport.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="报告URL" :span="2">{{ currentReport.reportFileUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="JSON" :span="2">
            <pre style="white-space: pre-wrap;">{{ currentReport.reportJson || '-' }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <el-form v-else :model="editForm" label-width="100px">
          <el-form-item label="报告编号">
            <el-input v-model="editForm.reportNo" />
          </el-form-item>
          <el-form-item label="检测机构">
            <el-input v-model="editForm.agency" />
          </el-form-item>
          <el-form-item label="结论">
            <el-input v-model="editForm.result" />
          </el-form-item>
          <el-form-item label="报告URL">
            <el-input v-model="editForm.reportFileUrl" />
          </el-form-item>
          <el-form-item label="JSON">
            <div class="json-template-row">
              <el-select v-model="editTemplateKey" style="width: 220px" placeholder="选择示例模板">
                <el-option
                  v-for="item in jsonTemplateOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button @click="applyJsonTemplate('edit', editForm, editTemplateKey)">应用模板</el-button>
            </div>
            <el-input v-model="editForm.reportJson" type="textarea" :rows="8" />
            <div class="json-tool-row">
              <el-button link type="primary" @click="formatJsonInput('edit', editForm)">格式化JSON</el-button>
              <el-button link @click="fillJsonExample('edit', editForm)">填充示例</el-button>
              <el-button link type="danger" @click="clearJsonInput('edit', editForm)">清空</el-button>
            </div>
            <div class="json-valid-row">
              <el-tag :type="isJsonValid(editForm.reportJson) ? 'success' : 'danger'" size="small">
                {{ isJsonValid(editForm.reportJson) ? 'JSON格式合法' : 'JSON格式有误' }}
              </el-tag>
            </div>
          </el-form-item>
        </el-form>
      </template>
    </div>

    <template #footer>
      <el-button @click="detailDialogVisible = false">关闭</el-button>
      <el-button v-if="detailMode === 'edit'" type="primary" :loading="submitting" @click="handleUpdate">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useQualityManage } from '../../composables/useQualityManage'

const {
  canWrite,
  form,
  uploading,
  submitting,
  uploadedUrl,
  queryBatchId,
  listLoading,
  reportList,
  detailDialogVisible,
  detailLoading,
  detailMode,
  currentReport,
  editForm,
  handleFileChange,
  handleUpload,
  handleSubmit,
  jsonTemplateOptions,
  formatJsonInput,
  applyJsonTemplate,
  fillJsonExample,
  clearJsonInput,
  isJsonValid,
  loadReportList,
  openDetail,
  handleUpdate
} = useQualityManage()

const createTemplateKey = ref('default')
const editTemplateKey = ref('default')
</script>

<style src="./quality-manage.css" scoped></style>
