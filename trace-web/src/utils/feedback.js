export function extractErrorMessage(error, fallback = '加载失败，请稍后重试') {
  const message =
    error?.response?.data?.message ||
    error?.message ||
    ''
  if (!message || message === 'Network Error') {
    return fallback
  }
  return message
}

export function buildActionErrorMessage(action, error, fallback) {
  return `${action}：${extractErrorMessage(error, fallback || `${action}失败，请稍后重试`)}`
}
