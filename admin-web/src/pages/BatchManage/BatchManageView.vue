<template>
  <el-row :gutter="20" class="batch-manage-page">
    <el-col v-if="canCreate" :span="8">
      <el-card>
        <template #header>
          <span>新建批次</span>
        </template>

        <el-form :model="form" label-width="90px">
          <el-form-item label="产品ID">
            <el-input-number v-model="form.productId" :min="1" class="full-width" />
          </el-form-item>

          <el-form-item label="开始日期">
            <el-date-picker
              v-model="form.startDate"
              type="date"
              placeholder="请选择开始日期"
              value-format="YYYY-MM-DD"
              class="full-width"
            />
          </el-form-item>

          <el-form-item label="产地">
            <div class="origin-place-wrap">
              <el-input
                  v-model="form.originPlace"
                  placeholder="请点击右侧按钮自动获取"
                  readonly
              />
              <el-button
                  type="primary"
                  plain
                  :loading="locating"
                  @click="handleGetLocation"
              >
                获取当前位置
              </el-button>
            </div>
          </el-form-item>

          <el-form-item label="位置地图">
            <div class="map-container" v-if="mapPreviewUrl">
              <img
                  :src="mapPreviewUrl"
                  alt="位置地图"
                  class="map-image"
              />
            </div>
            <div class="map-empty" v-else>
              请输入产地或点击获取当前位置后显示地图
            </div>
          </el-form-item>

          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleCreate">
              创建批次
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :span="canCreate ? 16 : 24">
      <el-card>
        <template #header>
          <span>批次列表</span>
        </template>

        <el-table
            :data="tableData"
            border
            class="batch-table"
            @row-click="handleRowClick"
            empty-text="暂无批次数据"
        >
          <el-table-column prop="id" label="ID" min-width="180" />
          <el-table-column prop="batchCode" label="批次编码" min-width="180" />
          <el-table-column prop="productName" label="产品名称" min-width="140" />
          <el-table-column prop="companyId" label="企业ID" width="100" />
          <el-table-column prop="originPlace" label="产地" min-width="140" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getBatchStatusTagType(row.status)" size="small">
                {{ getBatchStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="handleRowClick(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :current-page="current"
              :page-size="size"
              @current-change="handlePageChange"
          />
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { useBatchManage } from '../../composables/useBatchManage'
import { getBatchStatusTagType, getBatchStatusText } from '../../utils/display'

const {
  form,
  canCreate,
  loading,
  locating,
  current,
  size,
  total,
  tableData,
  mapPreviewUrl,
  handleCreate,
  handleGetLocation,
  handlePageChange,
  handleRowClick
} = useBatchManage()
</script>

<style src="./batch-manage.css" scoped></style>
