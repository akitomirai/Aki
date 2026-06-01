import QRCode from 'qrcode'

const DEFAULT_QR_COLOR = '#1f3f68'
const DEFAULT_ACCENT_COLOR = '#6ca8d9'
const DEFAULT_LOGO_SRC = '/images/brand/system-icon.jpg'

function roundedRect(ctx, x, y, width, height, radius) {
  const r = Math.min(radius, width / 2, height / 2)
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + width, y, x + width, y + height, r)
  ctx.arcTo(x + width, y + height, x, y + height, r)
  ctx.arcTo(x, y + height, x, y, r)
  ctx.arcTo(x, y, x + width, y, r)
  ctx.closePath()
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = src
  })
}

function drawImageCover(ctx, image, x, y, size, radius) {
  const sourceSize = Math.min(image.naturalWidth || image.width, image.naturalHeight || image.height)
  const sourceX = Math.round(((image.naturalWidth || image.width) - sourceSize) / 2)
  const sourceY = Math.round(((image.naturalHeight || image.height) - sourceSize) / 2)

  ctx.save()
  roundedRect(ctx, x, y, size, size, radius)
  ctx.clip()
  ctx.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, x, y, size, size)
  ctx.restore()
}

function drawFallbackLogo(ctx, x, y, size) {
  const centerX = x + size / 2
  const centerY = y + size / 2
  const shieldRadius = size * 0.38

  const gradient = ctx.createLinearGradient(x, y, x + size, y + size)
  gradient.addColorStop(0, DEFAULT_ACCENT_COLOR)
  gradient.addColorStop(1, DEFAULT_QR_COLOR)

  ctx.save()
  ctx.fillStyle = gradient
  ctx.beginPath()
  ctx.moveTo(centerX, y + size * 0.08)
  ctx.quadraticCurveTo(x + size * 0.86, y + size * 0.17, x + size * 0.82, y + size * 0.48)
  ctx.quadraticCurveTo(x + size * 0.78, y + size * 0.78, centerX, y + size * 0.92)
  ctx.quadraticCurveTo(x + size * 0.22, y + size * 0.78, x + size * 0.18, y + size * 0.48)
  ctx.quadraticCurveTo(x + size * 0.14, y + size * 0.17, centerX, y + size * 0.08)
  ctx.closePath()
  ctx.fill()

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.86)'
  ctx.lineWidth = Math.max(2, size * 0.055)
  ctx.beginPath()
  ctx.arc(centerX, centerY + size * 0.04, shieldRadius * 0.62, Math.PI * 0.08, Math.PI * 1.22)
  ctx.stroke()

  ctx.fillStyle = '#ffffff'
  ctx.beginPath()
  ctx.ellipse(centerX - size * 0.1, centerY - size * 0.02, size * 0.18, size * 0.32, -0.68, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.ellipse(centerX + size * 0.14, centerY + size * 0.05, size * 0.14, size * 0.27, 0.76, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()
}

async function drawCenterLogo(ctx, canvasSize, logoSrc) {
  const plateSize = Math.round(canvasSize * 0.27)
  const plateX = Math.round((canvasSize - plateSize) / 2)
  const plateY = Math.round((canvasSize - plateSize) / 2)
  const radius = Math.round(plateSize * 0.22)
  const logoSize = Math.round(plateSize * 0.64)
  const logoX = Math.round((canvasSize - logoSize) / 2)
  const logoY = Math.round((canvasSize - logoSize) / 2)
  const logoRadius = Math.round(logoSize * 0.18)

  ctx.save()
  ctx.shadowColor = 'rgba(15, 23, 42, 0.16)'
  ctx.shadowBlur = Math.round(canvasSize * 0.018)
  ctx.shadowOffsetY = Math.round(canvasSize * 0.006)
  ctx.fillStyle = '#ffffff'
  roundedRect(ctx, plateX, plateY, plateSize, plateSize, radius)
  ctx.fill()
  ctx.restore()

  ctx.save()
  ctx.strokeStyle = 'rgba(108, 168, 217, 0.36)'
  ctx.lineWidth = Math.max(2, Math.round(canvasSize * 0.006))
  roundedRect(ctx, plateX, plateY, plateSize, plateSize, radius)
  ctx.stroke()
  ctx.restore()

  try {
    if (!logoSrc) {
      throw new Error('Logo image is not configured.')
    }
    const logo = await loadImage(logoSrc)
    drawImageCover(ctx, logo, logoX, logoY, logoSize, logoRadius)
  } catch {
    drawFallbackLogo(ctx, logoX, logoY, logoSize)
  }
}

export async function generateBrandedQrDataUrl(value, options = {}) {
  const width = options.width || 260
  const logoSrc = options.logoSrc || DEFAULT_LOGO_SRC
  const canvas = document.createElement('canvas')

  await QRCode.toCanvas(canvas, value, {
    width,
    margin: options.margin ?? 2,
    errorCorrectionLevel: 'H',
    color: {
      dark: options.darkColor || DEFAULT_QR_COLOR,
      light: options.lightColor || '#ffffff'
    }
  })

  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return canvas.toDataURL('image/png')
  }

  await drawCenterLogo(ctx, canvas.width, logoSrc)
  return canvas.toDataURL('image/png')
}
