import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..', '..', '..')

export const adminBaseUrl = process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174'
export const traceBaseUrl = process.env.TRACE_BASE_URL || 'http://127.0.0.1:5173'
export const apiBaseUrl = process.env.API_BASE_URL || 'http://127.0.0.1:8080/api'
export const screenshotDir = path.join(rootDir, 'artifacts', 'screenshots')

export async function saveNamedScreenshot(page, name) {
  await fs.mkdir(screenshotDir, { recursive: true })
  await page.screenshot({
    path: path.join(screenshotDir, `${name}.png`),
    fullPage: true
  })
}
