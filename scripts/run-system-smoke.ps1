param(
    [switch]$KeepServices,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$root = Get-ProjectRoot
$testDir = Join-Path $root 'tests\e2e'
$logsDir = Join-Path $root 'artifacts\logs'
$jsonReportPath = Join-Path $logsDir 'playwright-results.json'
$playwrightPackagePath = Join-Path $testDir 'node_modules\@playwright\test'
$browserInstallRoot = Join-Path $env:LOCALAPPDATA 'ms-playwright'
$requiredSpecs = @(
    'specs/admin-batch-smoke.spec.mjs',
    'specs/risk-workbench-smoke.spec.mjs',
    'specs/public-trace-smoke.spec.mjs',
    'specs/regulator-readonly-smoke.spec.mjs',
    'specs/quality-to-publish.spec.mjs',
    'specs/invalid-status-transition.spec.mjs',
    'specs/field-work-round.spec.mjs'
)

function Ensure-SystemSmokeDependencies {
    if (-not (Test-Path $playwrightPackagePath) -and -not $SkipInstall) {
        Write-Host 'Installing Playwright test dependencies...' -ForegroundColor Cyan
        Push-Location $testDir
        try {
            npm install
            if ($LASTEXITCODE -ne 0) {
                throw 'npm install failed in tests/e2e.'
            }
        }
        finally {
            Pop-Location
        }
    }

    $chromiumInstalled = Get-ChildItem $browserInstallRoot -Filter 'chromium-*' -ErrorAction SilentlyContinue
    if (-not $chromiumInstalled) {
        Write-Host 'Installing Playwright Chromium browser...' -ForegroundColor Cyan
        Push-Location $testDir
        try {
            npm run install:browsers
            if ($LASTEXITCODE -ne 0) {
                throw 'Playwright browser install failed.'
            }
        }
        finally {
            Pop-Location
        }
    }
}

function Get-FailedSpecTitles {
    param(
        [Parameter(Mandatory = $true)]
        $Suites
    )

    $titles = @()
    foreach ($suite in @($Suites)) {
        foreach ($spec in @($suite.specs)) {
            if ($spec -and -not $spec.ok) {
                $titles += $spec.title
            }
        }
        if ($suite.suites) {
            $titles += Get-FailedSpecTitles -Suites $suite.suites
        }
    }
    return $titles
}

function Show-SystemSmokeSummary {
    if (-not (Test-Path $jsonReportPath)) {
        Write-Host "Playwright JSON report was not found: $jsonReportPath" -ForegroundColor Yellow
        return
    }

    $report = Get-Content -Path $jsonReportPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $failedTitles = Get-FailedSpecTitles -Suites $report.suites

    Write-Host ''
    Write-Host 'System smoke summary' -ForegroundColor Cyan
    Write-Host ("  expected : {0}" -f $report.stats.expected)
    Write-Host ("  unexpected : {0}" -f $report.stats.unexpected)
    Write-Host ("  flaky : {0}" -f $report.stats.flaky)
    Write-Host ("  skipped : {0}" -f $report.stats.skipped)
    Write-Host ("  report : {0}" -f $jsonReportPath)

    if ($failedTitles.Count -gt 0) {
        Write-Host '  failed cases:' -ForegroundColor Yellow
        $failedTitles | Sort-Object -Unique | ForEach-Object {
            Write-Host ("    - {0}" -f $_) -ForegroundColor Yellow
        }
    }
}

if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
}

Ensure-SystemSmokeDependencies

try {
    Write-Host 'Starting local demo services for system smoke...' -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'start-local.ps1')

    $env:ADMIN_BASE_URL = 'http://127.0.0.1:5174'
    $env:TRACE_BASE_URL = 'http://127.0.0.1:5173'
    $env:API_BASE_URL = 'http://127.0.0.1:8080/api'

    Write-Host 'Running 7-group system smoke suite...' -ForegroundColor Cyan
    Push-Location $testDir
    try {
        npm run test:system-smoke
        $testExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    Show-SystemSmokeSummary

    if ($testExitCode -ne 0) {
        throw "System smoke suite failed with exit code $testExitCode."
    }
}
finally {
    if (-not $KeepServices) {
        & (Join-Path $PSScriptRoot 'stop-local.ps1')
    }
}
