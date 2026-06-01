import { useRouter } from 'vue-router'

const accountFlows = [
  {
    roleName: '平台管理员',
    userScope: '系统运维、企业建档、全局用户与监管账号管理',
    openedBy: '现有平台管理员',
    managedBy: '平台管理员'
  },
  {
    roleName: '企业管理员',
    userScope: '本企业产品、批次、二维码、质量与风险处理',
    openedBy: '平台管理员在企业建档后开户',
    managedBy: '平台管理员，可继续管理本企业操作员'
  },
  {
    roleName: '现场操作员',
    userScope: '移动端现场作业、批次记录填报',
    openedBy: '本企业管理员',
    managedBy: '本企业管理员'
  },
  {
    roleName: '监管人员',
    userScope: '监管视角查询、质量与风险只读核验',
    openedBy: '平台管理员',
    managedBy: '平台管理员'
  }
]

export function useRegister() {
  const router = useRouter()

  function goLogin() {
    router.push('/login')
  }

  function goMobileLogin() {
    router.push('/mobile-login')
  }

  return {
    accountFlows,
    goLogin,
    goMobileLogin
  }
}
