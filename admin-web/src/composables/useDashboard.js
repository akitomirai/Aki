import { onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getDashboardStatsApi } from '../api/dashboard'

export function useDashboard() {
  const authStore = useAuthStore()

  const batchCount = ref(0)
  const qrCount = ref(0)
  const pvCount = ref(0)
  const uvCount = ref(0)

  const loadDashboardStats = async () => {
    try {
      const roleCode = authStore.user?.roleCode || ''
      const res = await getDashboardStatsApi(roleCode)
      if (String(res?.code) !== '0') {
        return
      }
      const data = res?.data || {}
      batchCount.value = Number(data.batchCount || 0)
      qrCount.value = Number(data.qrCount || 0)
      pvCount.value = Number(data.pvCount || 0)
      uvCount.value = Number(data.uvCount || 0)
    } catch (e) {
      // keep zero fallback to avoid dashboard crash when stats service unavailable
    }
  }

  onMounted(() => {
    loadDashboardStats()
  })

  return {
    batchCount,
    qrCount,
    pvCount,
    uvCount,
    loadDashboardStats
  }
}
