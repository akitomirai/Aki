import { ref } from 'vue'

export function useDashboard() {
    const batchCount = ref(12)
    const qrCount = ref(80)
    const pvCount = ref(356)
    const uvCount = ref(97)

    return {
        batchCount,
        qrCount,
        pvCount,
        uvCount
    }
}