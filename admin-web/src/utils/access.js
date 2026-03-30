export const ROLE_CODES = {
  PLATFORM_ADMIN: 'PLATFORM_ADMIN',
  ENTERPRISE_ADMIN: 'ENTERPRISE_ADMIN',
  OPERATOR: 'OPERATOR',
  REGULATOR: 'REGULATOR'
}

export const ALL_ADMIN_ROLES = Object.values(ROLE_CODES)

export function normalizeRoleCode(roleCode = '') {
  if (roleCode === 'ENTERPRISE_USER') {
    return ROLE_CODES.OPERATOR
  }
  if (roleCode === 'ADMIN') {
    return ROLE_CODES.ENTERPRISE_ADMIN
  }
  return roleCode
}

export function getRoleName(roleCode = '') {
  switch (normalizeRoleCode(roleCode)) {
    case ROLE_CODES.PLATFORM_ADMIN:
      return '平台管理员'
    case ROLE_CODES.ENTERPRISE_ADMIN:
      return '企业管理员'
    case ROLE_CODES.OPERATOR:
      return '现场操作员'
    case ROLE_CODES.REGULATOR:
      return '监管人员'
    default:
      return '系统用户'
  }
}

export function normalizeUser(user = {}) {
  if (!user || typeof user !== 'object') {
    return {}
  }
  const roleCode = normalizeRoleCode(user.roleCode)
  return {
    ...user,
    roleCode,
    roleName: user.roleName || getRoleName(roleCode)
  }
}

export function hasRoleAccess(roleCode, allowedRoles = []) {
  if (!allowedRoles?.length) {
    return true
  }
  return allowedRoles.includes(normalizeRoleCode(roleCode))
}

export function getDefaultRouteByRole(roleCode = '') {
  const normalizedRole = normalizeRoleCode(roleCode)
  if (normalizedRole === ROLE_CODES.OPERATOR) {
    return '/field-entry'
  }
  if (normalizedRole && ALL_ADMIN_ROLES.includes(normalizedRole)) {
    return '/dashboard'
  }
  return '/login'
}
