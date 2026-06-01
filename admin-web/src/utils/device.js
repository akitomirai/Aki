export function isMobileDevice() {
  if (typeof window === 'undefined') {
    return false
  }
  const userAgent = navigator.userAgent || ''
  const touchEnabled = navigator.maxTouchPoints > 1
  const narrowViewport = window.matchMedia('(max-width: 820px)').matches
  return /Android|iPhone|iPad|iPod|Mobile|HarmonyOS/i.test(userAgent) || (touchEnabled && narrowViewport)
}
