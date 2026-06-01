param(
    [switch]$SkipBrowserChecks
)

$ErrorActionPreference = 'Stop'

function Invoke-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptName
    )

    $target = Join-Path $PSScriptRoot $ScriptName
    if (-not (Test-Path $target)) {
        throw "Required script was not found: $target"
    }

    Write-Host ''
    Write-Host ("==> {0}" -f $ScriptName) -ForegroundColor Cyan
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $target
    if ($LASTEXITCODE -ne 0) {
        throw "$ScriptName failed with exit code $LASTEXITCODE."
    }
}

Invoke-Step -ScriptName 'stop-local.ps1'
Invoke-Step -ScriptName 'reset-demo-baseline.ps1'
Invoke-Step -ScriptName 'start-local.ps1'
Invoke-Step -ScriptName 'check-local.ps1'
Invoke-Step -ScriptName 'demo-precheck.ps1'

if (-not $SkipBrowserChecks) {
    Invoke-Step -ScriptName 'verify-final-demo-baseline.ps1'
}

Write-Host ''
Write-Host 'Final demo baseline is ready.' -ForegroundColor Green
