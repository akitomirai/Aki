$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$root = Get-ProjectRoot
$runtimeDir = Get-RuntimeDir
$backendLog = Get-ServiceLogPath -Name 'backend'
$runnerPath = Join-Path $PSScriptRoot 'local-service-runner.ps1'

Write-Host 'Resetting demo baseline by restarting backend with demo profile...' -ForegroundColor Cyan

$javaHome = Find-JavaHome
if (-not $javaHome) {
    throw 'JDK 17 or 21 was not found. Demo baseline reset cannot continue.'
}

New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null

$state = Read-LocalState
$managedBackend = $null
if ($state -and $state.services) {
    $managedBackend = $state.services.backend
}

if ($managedBackend -and (Test-ManagedServiceProcess -ServiceName 'backend' -ProcessId ([int]$managedBackend.pid))) {
    Stop-ProcessTree -RootProcessId ([int]$managedBackend.pid) | Out-Null
}
else {
    Stop-ProjectPortProcess -Port 8080
}

Start-Sleep -Seconds 2
Set-Content -Path $backendLog -Value '' -Encoding UTF8

$arguments = @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', "`"$runnerPath`"",
    '-Service', 'backend',
    '-ProjectRoot', "`"$root`"",
    '-LogPath', "`"$backendLog`"",
    '-JavaHome', "`"$javaHome`""
)

$process = Start-Process -FilePath 'powershell.exe' -ArgumentList ($arguments -join ' ') -WindowStyle Hidden -PassThru

if (-not (Wait-HttpOk -Url 'http://127.0.0.1:8080/actuator/health' -TimeoutSeconds 120)) {
    Write-Host ''
    Write-Host 'Backend log tail:' -ForegroundColor Yellow
    Show-LogTail -Path $backendLog -Tail 60 | Write-Host
    throw 'Demo baseline reset failed: backend did not return to healthy state in time.'
}

Update-StateServiceEntry -ServiceName 'backend' -ProcessId $process.Id -LogPath $backendLog | Out-Null

Write-Host 'Demo baseline reset complete.' -ForegroundColor Green
Write-Host "Backend PID : $($process.Id)"
Write-Host "Backend Log : $backendLog"
Write-Host 'Seed baseline:' -ForegroundColor Cyan
Write-Host '  batch 2 = PUBLISHED'
Write-Host '  publishedAt = 2026-03-21 11:30'
Write-Host '  quality = PASS'
Write-Host '  qr token = orange-202603-d1'
Write-Host '  default assignee = operator_support'
