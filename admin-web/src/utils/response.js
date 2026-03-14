export function isSuccess(res) {
  return Number(res?.code) === 0
}

export function getToken(res) {
  return res?.token || res?.data?.token || ''
}
