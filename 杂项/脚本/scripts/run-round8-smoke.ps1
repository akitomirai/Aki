param(
    [switch]$KeepServices,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

Write-Host 'run-round8-smoke.ps1 is kept as a compatibility wrapper.' -ForegroundColor Yellow
Write-Host 'Forwarding to scripts\run-system-smoke.ps1 ...' -ForegroundColor Yellow

& (Join-Path $PSScriptRoot 'run-system-smoke.ps1') @PSBoundParameters
