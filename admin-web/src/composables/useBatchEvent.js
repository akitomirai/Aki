import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listEventApi, createEventApi } from '../api/event'

export function useBatchEvent(batchId) {
    const events = ref([])
    const eventLoading = ref(false)

    const eventForm = reactive({
        stage: 'PRODUCE',
        location: ''
    })

    const eventJsonText = ref('{\n  "fields": {\n    "workType": "施肥",\n    "amount": "10kg"\n  }\n}')

    async function loadEvents() {
        try {
            const res = await listEventApi(batchId.value)
            if (String(res.code) === '0') {
                events.value = res.data || []
            } else {
                ElMessage.error(res.message || '加载事件失败')
            }
        } catch {
            ElMessage.error('加载事件失败')
        }
    }

    async function handleCreateEvent() {
        let content
        try {
            content = JSON.parse(eventJsonText.value)
        } catch {
            ElMessage.error('内容JSON格式不正确')
            return
        }

        eventLoading.value = true
        try {
            const res = await createEventApi({
                batchId: batchId.value,
                stage: eventForm.stage,
                location: eventForm.location,
                content,
                attachments: []
            })

            if (String(res.code) === '0') {
                ElMessage.success('事件新增成功')
                eventForm.location = ''
                await loadEvents()
            } else {
                ElMessage.error(res.message || '新增事件失败')
            }
        } catch {
            ElMessage.error('新增事件失败')
        } finally {
            eventLoading.value = false
        }
    }

    return {
        events,
        eventLoading,
        eventForm,
        eventJsonText,
        loadEvents,
        handleCreateEvent
    }
}