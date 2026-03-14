import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '../api/auth'

export function useRegister() {
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)

    const form = reactive({
        username: '',
        password: '',
        confirmPassword: '',
        realName: '',
        phone: '',
        inviteCode: ''
    })

    onMounted(() => {
        if (route.query.inviteCode) {
            form.inviteCode = route.query.inviteCode
        }
    })

    async function handleRegister() {
        if (!form.username || !form.password || !form.confirmPassword || !form.realName || !form.phone || !form.inviteCode) {
            ElMessage.warning('请填写完整注册信息（含姓名、手机号、邀请码）')
            return
        }

        if (form.password !== form.confirmPassword) {
            ElMessage.warning('两次输入的密码不一致')
            return
        }

        loading.value = true

        try {
            const res = await registerApi({
                username: form.username,
                password: form.password,
                realName: form.realName,
                phone: form.phone,
                inviteCode: form.inviteCode
            })

            const code = String(res?.code ?? '')

            if (code === '0') {
                ElMessage.success('注册成功，请登录')
                router.push('/login')
            } else {
                ElMessage.error(res?.message || '注册失败')
            }
        } catch (error) {
            ElMessage.error(
                error?.response?.data?.message || '注册请求失败'
            )
        } finally {
            loading.value = false
        }
    }

    function goLogin() {
        router.push('/login')
    }

    return {
        form,
        loading,
        handleRegister,
        goLogin
    }
}