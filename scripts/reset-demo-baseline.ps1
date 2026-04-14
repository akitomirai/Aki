$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$root = Get-ProjectRoot
$runtimeDir = Get-RuntimeDir
$backendLog = Get-ServiceLogPath -Name 'backend'
$runnerPath = Join-Path $PSScriptRoot 'local-service-runner.ps1'

Write-Host 'Resetting final demo baseline by restarting backend with the demo profile...' -ForegroundColor Cyan

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
    Show-LogTail -Path $backendLog -Tail 80 | Write-Host
    throw 'Demo baseline reset failed: backend did not return to healthy state in time.'
}

Update-StateServiceEntry -ServiceName 'backend' -ProcessId $process.Id -LogPath $backendLog | Out-Null

Write-Host 'Final demo baseline reset complete.' -ForegroundColor Green
Write-Host ("Backend PID : {0}" -f $process.Id)
Write-Host ("Backend Log : {0}" -f $backendLog)
Write-Host ''
Write-Host 'Fixed demo accounts:' -ForegroundColor Cyan
Write-Host '  platform / 123456          -> PLATFORM_ADMIN'
Write-Host '  enterprise_admin / 123456  -> ENTERPRISE_ADMIN (company 1)'
Write-Host '  operator / 123456          -> OPERATOR (company 1)'
Write-Host '  regulator / 123456         -> REGULATOR'
Write-Host ''
Write-Host 'Fixed demo batches:' -ForegroundColor Cyan
Write-Host '  ORANGE-202603-D1 -> published star batch / public token orange-202603-d1'
Write-Host '  ORANGE-202604-Q1 -> pending quality / operator todo batch'
Write-Host '  TEA-202603-F1    -> frozen risk batch'
Write-Host '  RICE-202603-R1   -> recalled batch / public token demo-recall-2026'
