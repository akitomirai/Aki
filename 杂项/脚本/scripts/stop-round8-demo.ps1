param(
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

if (-not $Quiet) {
    Write-Host 'stop-round8-demo.ps1 is now a compatibility wrapper.' -ForegroundColor Yellow
    Write-Host 'Forwarding to scripts\stop-local.ps1 ...' -ForegroundColor Yellow
}

& (Join-Path $PSScriptRoot 'stop-local.ps1')
