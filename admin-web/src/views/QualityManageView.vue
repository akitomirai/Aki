<template>
  <AdminLayout>
    <el-row :gutter="20">
      <el-col :span="10">
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

          <div style="margin-top: 16px;">
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

      <el-col :span="14">
        <el-card>
          <template #header>
            <span>新增质检报告（自动存证）</span>
          </template>

          <el-form :model="form" label-width="100px">
            <el-form-item label="批次ID">
              <el-input v-model="form.batchId" placeholder="请输入批次ID（字符串）" />
            </el-form-item>

            <el-form-item label="报告编号">
              <el-input v-model="form.reportNo" placeholder="如：R-2026-001" />
            </el-form-item>

            <el-form-item label="检测机构">
              <el-input v-model="form.agency" placeholder="如：赣州某检测机构" />
            </el-form-item>

            <el-form-item label="检测结果">
              <el-select v-model="form.result" style="width: 100%;">
                <el-option label="合格" value="合格" />
                <el-option label="不合格" value="不合格" />
              </el-select>
            </el-form-item>

            <el-form-item label="报告URL">
              <el-input v-model="form.reportFileUrl" placeholder="上传后自动填充，也可手动粘贴" />
            </el-form-item>

            <el-form-item label="扩展JSON">
              <el-input
                  v-model="form.reportJson"
                  type="textarea"
                  :rows="6"
                  placeholder='例如：{"items":[{"name":"农残","value":"合格"}]}'
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">
                提交质检报告
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </AdminLayout>
</template>

<script setup>
/**
 * 质检报告管理页：
 * - 上传报告文件
 * - 提交报告信息
 * - 后端会自动生成 hash_notary 存证
 */
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '../components/AdminLayout.vue'
import { uploadFileApi } from '../api/file'
import { createQualityReportApi } from '../api/quality'

const selectedFile = ref(null)
const uploading = ref(false)
const submitting = ref(false)
const uploadedUrl = ref('')

const form = reactive({
  batchId: '',
  reportNo: '',
  agency: '',
  result: '合格',
  reportFileUrl: '',
  reportJson: '{\n  "items": [\n    {\n      "name": "农残",\n      "value": "合格"\n    }\n  ]\n}'
})

/**
 * 选择文件
 */
function handleFileChange(file) {
  selectedFile.value = file.raw
}

/**
 * 上传文件
 */
async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('biz', 'quality')

  uploading.value = true
  try {
    const res = await uploadFileApi(formData)
    if (res.code === 0) {
      uploadedUrl.value = res.data.url
      form.reportFileUrl = res.data.url
      ElMessage.success('文件上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (error) {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}

/**
 * 提交质检报告
 */
async function handleSubmit() {
  if (!form.batchId) {
    ElMessage.warning('请输入批次ID')
    return
  }
  if (!form.result) {
    ElMessage.warning('请选择检测结果')
    return
  }
  if (!form.reportFileUrl) {
    ElMessage.warning('请先上传文件或填写报告URL')
    return
  }

  submitting.value = true
  try {
    const res = await createQualityReportApi({
      batchId: form.batchId,
      reportNo: form.reportNo,
      agency: form.agency,
      result: form.result,
      reportFileUrl: form.reportFileUrl,
      reportJson: form.reportJson
    })

    if (res.code === 0) {
      ElMessage.success('质检报告提交成功，已完成哈希存证')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交质检报告失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.upload-box {
  margin-bottom: 12px;
}

.result-box {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.url-text {
  word-break: break-all;
  font-size: 12px;
  color: #409eff;
}
</style>