<template>
  <div class="trace-page">
    <div class="trace-container">
      <header class="trace-header">
        <div>
          <p class="trace-header__eyebrow">Public Traceability</p>
          <h1 class="trace-header__title">农产品溯源详情</h1>
          <p class="trace-header__desc">扫码后可查看批次、过程记录、质检情况与监管信息。</p>
          <el-tag v-if="qrToken" size="small" effect="plain">追溯码：{{ qrToken }}</el-tag>
        </div>
      </header>

      <el-card v-if="loading" class="trace-card">
        <el-skeleton :rows="8" animated />
      </el-card>

      <el-card v-else-if="errorMessage" class="trace-card trace-card--error" shadow="never">
        <el-result icon="error" title="查询失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="loadTraceDetail">重新查询</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else-if="detail">
        <!-- 风险提示区 -->
        <el-alert
          v-if="detail.riskMessage"
          :title="riskTitle"
          :description="riskText"
          :type="riskType"
          show-icon
          :closable="false"
          class="trace-alert"
        />

        <!-- 顶部产品核心信息 -->
        <section class="trace-section">
          <el-card class="trace-card trace-hero" shadow="never">
            <div class="trace-hero__content">
              <div class="trace-hero__main">
                <span class="trace-hero__tag">溯源档案</span>
                <h2 class="trace-hero__title">{{ detail.productName || '未命名产品' }}</h2>
                <div class="trace-hero__meta">
                  <span><i class="el-icon-office-building"></i> {{ detail.companyName || '-' }}</span>
                  <el-divider direction="vertical" />
                  <span><i class="el-icon-location"></i> {{ detail.originPlace || '-' }}</span>
                </div>
              </div>
              <div class="trace-hero__status">
                <el-tag :type="getStatusTagType(detail.batchStatus)" size="large" effect="dark">
                  {{ getStatusText(detail.batchStatus) }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </section>

        <!-- 批次基础信息与状态 -->
        <section class="trace-section">
          <div class="trace-grid">
            <el-card class="trace-card" shadow="never">
              <template #header>
                <div class="trace-card__header">
                  <i class="el-icon-info"></i>
                  <span>基础信息</span>
                </div>
              </template>
              <div class="trace-info-list">
                <div v-for="item in productInfoList" :key="item.label" class="trace-info-row">
                  <span class="trace-info-row__label">{{ item.label }}</span>
                  <span class="trace-info-row__value">{{ item.value }}</span>
                </div>
              </div>
            </el-card>

            <el-card class="trace-card" shadow="never">
              <template #header>
                <div class="trace-card__header">
                  <i class="el-icon-set-up"></i>
                  <span>状态说明</span>
                </div>
              </template>
              <div class="trace-status-list">
                <div v-for="item in statusInfoList" :key="item.label" class="trace-status-row">
                  <span class="trace-status-row__label">{{ item.label }}</span>
                  <div class="trace-status-row__content">
                    <el-tag v-if="item.raw" :type="item.tagType" size="small" effect="light" round>
                      {{ item.text }}
                    </el-tag>
                    <span v-else class="trace-status-text">{{ item.text }}</span>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </section>

        <!-- 生产全过程（时间轴） -->
        <section class="trace-section">
          <el-card class="trace-card" shadow="never">
            <template #header>
              <div class="trace-card__header">
                <i class="el-icon-time"></i>
                <span>全流程追溯记录</span>
              </div>
            </template>

            <el-empty v-if="eventList.length === 0" :image-size="60" description="暂无过程记录" />

            <el-timeline v-else class="trace-timeline">
              <el-timeline-item
                v-for="(item, index) in eventList"
                :key="index"
                :timestamp="item.eventTime"
                placement="top"
                :type="index === 0 ? 'primary' : ''"
              >
                <div class="trace-event-item">
                  <div class="trace-event-item__header">
                    <span class="trace-event-item__stage">{{ getStageText(item.stage) }}</span>
                    <h4 class="trace-event-item__title">{{ item.title }}</h4>
                  </div>
                  <div v-if="item.location" class="trace-event-item__loc">
                    <i class="el-icon-location-outline"></i> {{ item.location }}
                  </div>
                  <div v-if="getEventFields(item).length > 0" class="trace-event-fields">
                    <div v-for="field in getEventFields(item)" :key="field.key" class="trace-event-field">
                      <span class="label">{{ field.label }}:</span>
                      <span class="value">{{ field.value }}</span>
                    </div>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </section>

        <!-- 关键业务节点 -->
        <section class="trace-section">
          <el-card class="trace-card" shadow="never">
            <template #header>
              <div class="trace-card__header">
                <i class="el-icon-connection"></i>
                <span>关键节点摘要</span>
              </div>
            </template>

            <el-empty v-if="nodeList.length === 0" :image-size="60" description="暂无关键环节记录" />

            <div v-else class="trace-node-list">
              <div v-for="(item, index) in nodeList" :key="index" class="trace-node-item">
                <div class="trace-node-item__side">
                  <div class="trace-node-item__dot"></div>
                  <div v-if="index !== nodeList.length - 1" class="trace-node-item__line"></div>
                </div>
                <div class="trace-node-item__main">
                  <div class="trace-node-item__header">
                    <span class="trace-node-item__type">{{ getNodeTypeText(item.nodeType) }}</span>
                    <span class="trace-node-item__time">{{ item.eventTime }}</span>
                  </div>
                  <h4 class="trace-node-item__title">{{ item.title || '记录节点' }}</h4>
                  <p class="trace-node-item__corp">责任企业：{{ item.companyName }} ({{ getBizRoleText(item.bizRole) }})</p>
                  <p v-if="item.content" class="trace-node-item__content">
                    内容摘要：{{ parseNodeContent(item.content) }}
                  </p>
                </div>
              </div>
            </div>
          </el-card>
        </section>

        <!-- 参与企业列表 -->
        <section class="trace-section">
          <el-card class="trace-card" shadow="never">
            <template #header>
              <div class="trace-card__header">
                <i class="el-icon-user"></i>
                <span>参与主体信息</span>
              </div>
            </template>

            <el-empty v-if="participantList.length === 0" :image-size="60" description="暂无参与主体信息" />

            <div v-else class="trace-participant-grid">
              <div v-for="(item, index) in participantList" :key="index" class="trace-participant-card">
                <div class="trace-participant-card__body">
                  <div class="trace-participant-card__icon">
                    <i class="el-icon-office-building"></i>
                  </div>
                  <div class="trace-participant-card__info">
                    <div class="name-row">
                      <span class="name">{{ item.companyName }}</span>
                      <el-tag v-if="item.isCreator" type="success" size="mini" effect="plain">创建方</el-tag>
                    </div>
                    <div class="role-row">
                      <span class="role-tag">{{ getBizRoleText(item.bizRole) }}</span>
                      <span v-if="item.stageOrder !== null" class="order">环节顺序: {{ item.stageOrder }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </section>

        <!-- 质检与监管 -->
        <section class="trace-section">
          <div class="trace-grid">
            <!-- 质检报告 -->
            <el-card class="trace-card" shadow="never">
              <template #header>
                <div class="trace-card__header">
                  <i class="el-icon-document-checked"></i>
                  <span>质检报告</span>
                </div>
              </template>

              <el-empty v-if="qualityReportList.length === 0" :image-size="40" description="暂无质检信息" />

              <div v-else class="trace-mini-list">
                <div v-for="(item, index) in qualityReportList" :key="index" class="trace-mini-item">
                  <div class="trace-mini-item__head">
                    <el-tag :type="getQualityResultTagType(item.result)" size="mini">
                      {{ getQualityResultText(item.result) }}
                    </el-tag>
                    <span class="no">#{{ item.reportNo }}</span>
                  </div>
                  <div class="trace-mini-item__body">
                    <p>检测机构：{{ item.agency }}</p>
                    <p>检测日期：{{ formatShortDate(item.reportDate) }}</p>
                  </div>
                  <el-button v-if="item.reportFileUrl" type="text" size="mini" @click="openReport(item)">查看报告原文</el-button>
                </div>
              </div>
            </el-card>

            <!-- 监管记录 -->
            <el-card class="trace-card" shadow="never">
              <template #header>
                <div class="trace-card__header">
                  <i class="el-icon-view"></i>
                  <span>监管记录</span>
                </div>
              </template>

              <el-empty v-if="regulationRecordList.length === 0" :image-size="40" description="暂无监管记录" />

              <div v-else class="trace-mini-list">
                <div v-for="(item, index) in regulationRecordList" :key="index" class="trace-mini-item">
                  <div class="trace-mini-item__head">
                    <el-tag :type="getRegulationResultTagType(item.inspectResult)" size="mini">
                      {{ getStatusText(item.inspectResult) }}
                    </el-tag>
                    <span class="time">{{ formatShortDate(item.inspectTime) }}</span>
                  </div>
                  <div class="trace-mini-item__body">
                    <p>处理措施：{{ item.actionTaken || '常规检查' }}</p>
                    <p>监管人员：{{ item.inspectorName }}</p>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </section>

        <!-- 消费者反馈 -->
        <section class="trace-section">
          <el-card class="trace-card trace-feedback" shadow="never">
            <template #header>
              <div class="trace-card__header">
                <i class="el-icon-message"></i>
                <span>我要反馈</span>
              </div>
            </template>

            <el-form :model="feedbackForm" label-position="top" size="small">
              <el-form-item label="反馈类型">
                <el-radio-group v-model="feedbackForm.feedbackType">
                  <el-radio-button label="SUGGESTION">建议</el-radio-button>
                  <el-radio-button label="COMPLAINT">投诉</el-radio-button>
                  <el-radio-button label="REPORT_RISK">举报</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="反馈内容" required>
                <el-input
                  v-model="feedbackForm.content"
                  type="textarea"
                  :rows="3"
                  placeholder="您的意见对我们很重要..."
                />
              </el-form-item>

              <div class="trace-feedback-row">
                <el-form-item label="姓名" class="flex-1">
                  <el-input v-model="feedbackForm.contactName" placeholder="可选" />
                </el-form-item>
                <el-form-item label="电话" class="flex-1">
                  <el-input v-model="feedbackForm.contactPhone" placeholder="可选" />
                </el-form-item>
              </div>

              <el-button
                type="primary"
                class="w-full"
                :loading="submittingFeedback"
                @click="submitFeedback"
              >
                提交反馈
              </el-button>
            </el-form>
          </el-card>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import './trace-detail.css'
import { useTraceDetail } from '../../composables/useTraceDetail'

const {
  loading,
  errorMessage,
  detail,
  qrToken,
  riskType,
  riskTitle,
  riskText,
  productInfoList,
  statusInfoList,
  eventList,
  participantList,
  nodeList,
  qualityReportList,
  pesticideRecordList,
  regulationRecordList,
  feedbackForm,
  submittingFeedback,
  submitFeedback,
  loadTraceDetail,
  getStatusText,
  getStatusTagType,
  getQualityResultText,
  getQualityResultTagType,
  getRegulationResultTagType,
  formatShortDate,
  getStageText,
  getBizRoleText,
  getNodeTypeText,
  getEventFields,
  parseNodeContent,
  openReport,
  formatJson
} = useTraceDetail()
</script>
