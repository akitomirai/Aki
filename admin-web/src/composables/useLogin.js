import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '../api/auth'
import { useAuthStore } from '../stores/auth'

export function useLogin() {
    const router = useRouter()
    const authStore = useAuthStore()

    const loading = ref(false)
    const remember = ref(true)

    const form = reactive({
        username: '',
        password: ''
    })

    const USER_KEY = 'remember_username'

    onMounted(() => {
        const savedUser = localStorage.getItem(USER_KEY)

        if (savedUser) {
            form.username = savedUser
            remember.value = true
        } else {
            form.username = 'platform'
        }
    })

    async function handleLogin() {
        if (!form.username || !form.password) {
            ElMessage.warning('请输入用户名和密码')
            return
        }

        loading.value = true

        try {
            const res = await loginApi({
                username: form.username,
                password: form.password
            })

            const code = String(res?.code ?? '')
            const token = res?.data?.token
            const user = res?.data?.user

            if (code === '0' && token) {
                authStore.setAuth(token, user)

                if (remember.value) {
                    localStorage.setItem(USER_KEY, form.username)
                } else {
                    localStorage.removeItem(USER_KEY)
                }

                ElMessage.success('登录成功')
                router.push('/dashboard')
            } else {
                ElMessage.error(res?.message || '登录失败')
            }
        } catch (error) {
            ElMessage.error(
                error?.response?.data?.message || '登录请求失败'
            )
        } finally {
            loading.value = false
        }
    }

    function goRegister() {
        router.push('/register')
    }

    return {
        form,
        loading,
        remember,
        handleLogin,
        goRegister
    }
}