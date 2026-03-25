import { defineConfig } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..', '..')
const artifactsDir = path.join(rootDir, 'artifacts')

export default defineConfig({
  testDir: path.join(__dirname, 'specs'),
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  reporter: [
    ['list'],
    ['html', { outputFolder: path.join(artifactsDir, 'playwright-report'), open: 'never' }],
    ['json', { outputFile: path.join(artifactsDir, 'logs', 'playwright-results.json') }]
  ],
  outputDir: path.join(artifactsDir, 'test-results'),
  use: {
    baseURL: process.env.ADMIN_BASE_URL || 'http://127.0.0.1:5174',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
    screenshot: 'only-on-failure',
    testIdAttribute: 'data-testid'
  }
})
