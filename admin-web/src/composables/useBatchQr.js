import * as echarts from 'echarts'
import QRCode from 'qrcode'
import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listQrApi, generateQrApi, getQrTrendApi } from '../api/qr'

export function useBatchQr(batchId) {
    const qrList = ref([])
    const qrLoading = ref(false)

    const qrDialogVisible = ref(false)
    const qrImageUrl = ref('')
    const currentQrLink = ref('')
    const currentQrId = ref('')

    const trendDialogVisible = ref(false)
    const trendLoading = ref(false)
    const currentTrendQrId = ref('')
    const trendData = ref([])
    const trendChartRef = ref(null)

    let trendChartInstance = null

    const qrForm = reactive({
        count: 1,
        expireDays: 30,
        remark: '详情页生成'
    })

    async function loadQrList() {
        try {
            const res = await listQrApi(batchId.value)
            if (String(res.code) === '0') {
                qrList.value = res.data || []
            } else {
                ElMessage.error(res.message || '加载二维码失败')
            }
        } catch {
            ElMessage.error('加载二维码失败')
        }
    }

    async function handleGenerateQr() {
        qrLoading.value = true
        try {
            const res = await generateQrApi({
                batchId: batchId.value,
                count: qrForm.count,
                expireDays: qrForm.expireDays,
                remark: qrForm.remark
            })

            if (String(res.code) === '0') {
                ElMessage.success(`成功生成 ${res.data?.length || 0} 个二维码`)
                await loadQrList()
            } else {
                ElMessage.error(res.message || '生成二维码失败')
            }
        } catch {
            ElMessage.error('生成二维码失败')
        } finally {
            qrLoading.value = false
        }
    }

    function getTraceOrigin() {
        if (import.meta.env.VITE_TRACE_WEB_ORIGIN) {
            return import.meta.env.VITE_TRACE_WEB_ORIGIN
        }

        const origin = window.location.origin
        if (origin && !origin.includes('localhost') && !origin.includes('127.0.0.1')) {
            return origin
        }

        return 'http://localhost:5174'
    }

    function buildQrLink(row) {
        return `${getTraceOrigin()}/t/${row.qrToken}`
    }

    async function handleCopyQrLink(row) {
        const link = buildQrLink(row)
        try {
            await navigator.clipboard.writeText(link)
            ElMessage.success('二维码查询链接已复制')
        } catch {
            ElMessage.error('复制失败，请手动复制')
        }
    }

    async function handleCopyQrToken(row) {
        try {
            await navigator.clipboard.writeText(row.qrToken)
            ElMessage.success('二维码Token已复制')
        } catch {
            ElMessage.error('复制失败，请手动复制')
        }
    }

    function handleOpenQrLink(row) {
        const link = buildQrLink(row)
        window.open(link, '_blank')
    }

    async function handleShowQr(row) {
        const link = buildQrLink(row)
        currentQrLink.value = link
        currentQrId.value = row.id
        qrImageUrl.value = ''
        qrDialogVisible.value = true

        try {
            qrImageUrl.value = await QRCode.toDataURL(link, {
                width: 240,
                margin: 2
            })
        } catch {
            ElMessage.error('二维码生成失败')
        }
    }

    async function handleCopyCurrentQrLink() {
        if (!currentQrLink.value) return
        try {
            await navigator.clipboard.writeText(currentQrLink.value)
            ElMessage.success('链接已复制')
        } catch {
            ElMessage.error('复制失败')
        }
    }

    function handleDownloadQr() {
        if (!qrImageUrl.value) {
            ElMessage.warning('二维码尚未生成完成')
            return
        }

        const a = document.createElement('a')
        a.href = qrImageUrl.value
        a.download = `qr-${currentQrId.value || 'code'}.png`
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
    }

    async function handleShowTrend(row) {
        currentTrendQrId.value = row.id
        trendDialogVisible.value = true
        await loadQrTrend()
    }

    async function handleTrendDialogOpened() {
        await nextTick()
        renderTrendChart()
    }

    async function loadQrTrend() {
        if (!currentTrendQrId.value) return

        trendLoading.value = true
        try {
            const res = await getQrTrendApi(currentTrendQrId.value, 7)
            if (String(res.code) === '0') {
                trendData.value = res.data || []
                await nextTick()
                renderTrendChart()
            } else {
                ElMessage.error(res.message || '加载趋势失败')
            }
        } catch {
            ElMessage.error('加载趋势失败')
        } finally {
            trendLoading.value = false
            await nextTick()
            renderTrendChart()
        }
    }

    function renderTrendChart() {
        if (!trendChartRef.value || trendData.value.length === 0) return

        if (!trendChartInstance) {
            trendChartInstance = echarts.init(trendChartRef.value)
        }

        const xData = trendData.value.map(item => item.day)
        const pvData = trendData.value.map(item => Number(item.pv || 0))
        const hasUv = trendData.value.some(item => item.uv !== undefined && item.uv !== null)
        const uvData = trendData.value.map(item => Number(item.uv || 0))

        const series = [
            {
                name: 'PV',
                type: 'line',
                smooth: true,
                data: pvData
            }
        ]

        if (hasUv) {
            series.push({
                name: 'UV',
                type: 'line',
                smooth: true,
                data: uvData
            })
        }

        trendChartInstance.setOption({
            tooltip: { trigger: 'axis' },
            legend: { data: hasUv ? ['PV', 'UV'] : ['PV'] },
            xAxis: { type: 'category', data: xData },
            yAxis: { type: 'value' },
            series
        })
    }

    return {
        qrList,
        qrLoading,
        qrForm,
        qrDialogVisible,
        qrImageUrl,
        currentQrLink,
        currentQrId,
        trendDialogVisible,
        trendLoading,
        currentTrendQrId,
        trendData,
        trendChartRef,
        loadQrList,
        handleGenerateQr,
        handleCopyQrLink,
        handleCopyQrToken,
        handleOpenQrLink,
        handleShowQr,
        handleCopyCurrentQrLink,
        handleDownloadQr,
        handleShowTrend,
        handleTrendDialogOpened,
        loadQrTrend
    }
}