param(
    [switch]$NoWait
)

$ErrorActionPreference = 'Stop'

if ($NoWait) {
    Write-Host 'start-round7-demo.ps1 is now a compatibility wrapper. -NoWait is no longer needed and will be ignored.' -ForegroundColor Yellow
}
else {
    Write-Host 'start-round7-demo.ps1 is now a compatibility wrapper.' -ForegroundColor Yellow
}

Write-Host 'Forwarding to scripts\start-local.ps1 ...' -ForegroundColor Yellow
& (Join-Path $PSScriptRoot 'start-local.ps1')
