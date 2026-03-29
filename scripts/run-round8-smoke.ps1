param(
    [switch]$KeepServices,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\round8-demo-common.ps1"

$paths = Get-Round8Paths
$testDir = Join-Path (Get-Round8Root) 'tests\e2e'
$playwrightPackagePath = Join-Path $testDir 'node_modules\@playwright\test'
$chromiumInstalled = Get-ChildItem "$env:LOCALAPPDATA\ms-playwright" -Filter 'chromium-*' -ErrorAction SilentlyContinue

Ensure-Round8Directories
Remove-Round8TestArtifacts

try {
    if (-not $SkipInstall -and -not (Test-Path $playwrightPackagePath)) {
        Write-Host "Installing Playwright test dependencies..." -ForegroundColor Cyan
        Push-Location $testDir
        npm install
        if ($LASTEXITCODE -ne 0) {
            throw "npm install failed in tests/e2e."
        }
        Pop-Location
    }

    if (-not $chromiumInstalled) {
        Write-Host "Installing Playwright Chromium browser..." -ForegroundColor Cyan
        Push-Location $testDir
        npm run install:browsers
        if ($LASTEXITCODE -ne 0) {
            throw "Playwright browser install failed."
        }
        Pop-Location
    }

    Write-Host "Starting round 8 demo environment..." -ForegroundColor Cyan
    & "$PSScriptRoot\start-round7-demo.ps1"

    Write-Host "Running precheck..." -ForegroundColor Cyan
    & "$PSScriptRoot\demo-precheck.ps1"

    Write-Host "Running Playwright smoke tests..." -ForegroundColor Cyan
    $env:ADMIN_BASE_URL = 'http://127.0.0.1:5174'
    $env:TRACE_BASE_URL = 'http://127.0.0.1:5173'
    $env:API_BASE_URL = 'http://127.0.0.1:8080/api'

    Push-Location $testDir
    npm run test
    $testExitCode = $LASTEXITCODE
    Pop-Location

    Copy-Round8FailureArtifacts

    if ($testExitCode -ne 0) {
        throw "Playwright smoke tests failed with exit code $testExitCode."
    }

    Write-Host ""
    Write-Host "Round 8 smoke completed successfully." -ForegroundColor Green
    Write-Host ("  HTML report: {0}" -f $paths.Report)
    Write-Host ("  Screenshots: {0}" -f $paths.Screenshots)
    Write-Host ("  Test output: {0}" -f $paths.TestResults)
    Write-Host ("  Logs:        {0}" -f $paths.Logs)
} catch {
    Copy-Round8FailureArtifacts
    Write-Host ""
    Write-Host "Round 8 smoke failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ("Check logs first: {0}" -f $paths.Logs) -ForegroundColor Yellow
    throw
} finally {
    if (-not $KeepServices) {
        & "$PSScriptRoot\stop-round8-demo.ps1" -Quiet
    }
}
