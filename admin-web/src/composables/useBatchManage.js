import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createBatchApi, pageBatchApi } from '../api/batch'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { buildActionErrorMessage, extractErrorMessage } from '../utils/feedback'

export function useBatchManage() {
    const router = useRouter()
    const authStore = useAuthStore()
    const roleCode = computed(() => authStore.user?.roleCode || '')
    const canCreate = computed(() => roleCode.value === 'ENTERPRISE_ADMIN' || roleCode.value === 'ENTERPRISE_USER' || roleCode.value === 'ADMIN')
    const loading = ref(false)
    const locating = ref(false)
    const current = ref(1)
    const size = ref(10)
    const total = ref(0)
    const tableData = ref([])

    const form = reactive({
        productId: 1,
        originPlace: '',
        latitude: null,
        longitude: null,
        startDate: '',
        remark: '前端创建测试'
    })

    const AMAP_KEY = import.meta.env.VITE_AMAP_KEY

    const mapPreviewUrl = computed(() => {
        if (!form.longitude || !form.latitude || !AMAP_KEY) return ''

        const location = `${form.longitude},${form.latitude}`

        return `https://restapi.amap.com/v3/staticmap?location=${encodeURIComponent(location)}&zoom=14&size=420*420&markers=mid,0xFF0000,A:${encodeURIComponent(location)}&key=${AMAP_KEY}`
    })

    async function loadTable() {
        try {
            const res = await pageBatchApi({
                current: current.value,
                size: size.value
            }, roleCode.value)

            if (String(res.code) === '0') {
                tableData.value = res.data?.records || []
                total.value = Number(res.data?.total || 0)
            } else {
                ElMessage.error(`查询失败：${res.message || '未查询到数据'}`)
            }
        } catch (error) {
            ElMessage.error(buildActionErrorMessage('查询失败', error, '批次列表加载失败'))
        }
    }

    function getCurrentPosition() {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('当前浏览器不支持定位'))
                return
            }

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    resolve({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude
                    })
                },
                (error) => {
                    reject(error)
                },
                {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 0
                }
            )
        })
    }

    async function reverseGeocode(latitude, longitude) {
        if (!AMAP_KEY) {
            throw new Error('请先配置高德Key')
        }

        const location = `${longitude},${latitude}`

        const url =
            `https://restapi.amap.com/v3/geocode/regeo?key=${AMAP_KEY}&location=${encodeURIComponent(location)}&extensions=base`

        const response = await fetch(url)
        const data = await response.json()

        console.log('高德逆地理编码返回：', data)

        if (String(data.status) !== '1') {
            throw new Error(data.info || '地址解析失败')
        }

        const comp = data.regeocode?.addressComponent || {}

        const province = comp.province || ''
        const city = Array.isArray(comp.city) ? '' : (comp.city || '')
        const district = comp.district || ''

        return `${province} · ${city} · ${district}`
    }

    async function handleGetLocation() {
        locating.value = true
        form.originPlace = '正在获取当前位置...'

        try {
            const { latitude, longitude } = await getCurrentPosition()

            form.latitude = latitude
            form.longitude = longitude

            const address = await reverseGeocode(latitude, longitude)
            form.originPlace = address

            ElMessage.success('已自动获取当前位置')
        } catch (error) {
            console.error('定位失败：', error)

            form.originPlace = ''
            form.latitude = null
            form.longitude = null

            let msg = '定位失败'
            if (error?.code === 1) msg = '你拒绝了定位权限'
            if (error?.code === 2) msg = '无法获取当前位置'
            if (error?.code === 3) msg = '定位超时'
            if (error?.message) msg = error.message

            ElMessage.error(msg)
        } finally {
            locating.value = false
        }
    }

    async function handleCreate() {
        if (!canCreate.value) {
            ElMessage.warning('当前角色无权创建批次')
            return
        }
        if (!form.originPlace || form.originPlace === '正在获取当前位置...') {
            ElMessage.warning('请先点击“获取当前位置”自动填写产地')
            return
        }
        if (!form.startDate) {
            ElMessage.warning('请选择开始日期')
            return
        }

        loading.value = true
        try {
            const res = await createBatchApi({
                productId: form.productId,
                originPlace: form.originPlace,
                startDate: form.startDate,
                remark: form.remark
            }, roleCode.value)

            if (String(res.code) === '0') {
                ElMessage.success('批次创建成功')

                form.originPlace = ''
                form.latitude = null
                form.longitude = null
                form.startDate = ''
                form.remark = '前端创建测试'

                current.value = 1
                await loadTable()
            } else {
                ElMessage.error(`保存失败：${res.message || '批次创建失败'}`)
            }
        } catch (error) {
            ElMessage.error(`保存失败：${extractErrorMessage(error, '批次创建失败')}`)
        } finally {
            loading.value = false
        }
    }

    async function handlePageChange(page) {
        current.value = page
        await loadTable()
    }

    function handleRowClick(row) {
        if (!row?.id) return
        router.push(`/batches/${row.id}`)
    }

    onMounted(() => {
        loadTable()
    })

    return {
        form,
        roleCode,
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
    }
}
