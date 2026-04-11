$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$root = Get-ProjectRoot
$runtimeDir = Get-RuntimeDir
$runnerPath = Join-Path $PSScriptRoot 'local-service-runner.ps1'
$javaHome = Find-JavaHome

if (-not $javaHome) {
    throw 'JDK 17 or 21 was not found. Local start cannot continue.'
}

if (-not (Test-Path $runnerPath)) {
    throw "Local service runner was not found: $runnerPath"
}

try {
    $stoppedPortRecords = Resolve-RequiredPortConflicts -Ports @(8080, 5174, 5173) -AutoStopWorkspaceProcesses
    if ($stoppedPortRecords.Count -gt 0) {
        Write-Host 'Stopped existing workspace/demo listeners before local start:' -ForegroundColor Yellow
        $stoppedPortRecords | Select-Object Port, Pid, ProcessName | Format-Table -AutoSize
        Write-Host ''
    }
}
catch {
    Write-Host 'One or more required ports are already in use.' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ''
    Write-Host "If those listeners belong to this workspace, run scripts\stop-local.ps1 and try again." -ForegroundColor Yellow
    exit 1
}

$services = @(
    [pscustomobject]@{
        Name = 'backend'
        LogPath = Get-ServiceLogPath -Name 'backend'
    }
    [pscustomobject]@{
        Name = 'adminWeb'
        LogPath = Get-ServiceLogPath -Name 'adminWeb'
    }
    [pscustomobject]@{
        Name = 'traceWeb'
        LogPath = Get-ServiceLogPath -Name 'traceWeb'
    }
)

foreach ($service in $services) {
    Set-Content -Path $service.LogPath -Value '' -Encoding UTF8
}

$started = @()
try {
    foreach ($service in $services) {
        $arguments = @(
            '-NoProfile',
            '-ExecutionPolicy', 'Bypass',
            '-File', "`"$runnerPath`"",
            '-Service', $service.Name,
            '-ProjectRoot', "`"$root`"",
            '-LogPath', "`"$($service.LogPath)`""
        )
        if ($service.Name -eq 'backend') {
            $arguments += @('-JavaHome', "`"$javaHome`"")
        }

        $process = Start-Process -FilePath 'powershell.exe' -ArgumentList ($arguments -join ' ') -WindowStyle Hidden -PassThru
        $config = Get-ServiceConfig -Name $service.Name
        $started += [pscustomobject]@{
            Name = $service.Name
            DisplayName = $config.DisplayName
            Port = $config.Port
            Url = $config.Url
            ProbeUrl = $config.ProbeUrl
            LogPath = $service.LogPath
            Process = $process
        }
    }

    foreach ($service in $started) {
        $probe = Wait-HttpResult -Url $service.ProbeUrl -TimeoutSeconds 120
        if (-not $probe.Ok) {
            Write-Host ''
            Write-Host "Service failed to become healthy: $($service.DisplayName)" -ForegroundColor Red
            Write-Host "Probe URL : $($service.ProbeUrl)"
            Write-Host "Log Path  : $($service.LogPath)"
            Show-LogTail -Path $service.LogPath | Write-Host
            throw "Startup failed for $($service.DisplayName)."
        }
    }
}
catch {
    foreach ($service in $started) {
        Stop-ProcessTree -RootProcessId $service.Process.Id | Out-Null
    }
    Remove-LocalState
    throw
}

$state = [pscustomobject]@{
    createdAt = (Get-Date).ToString('s')
    projectRoot = $root
    runtimeDir = $runtimeDir
    services = [pscustomobject]@{}
}

$serviceState = [ordered]@{}
foreach ($service in $started) {
    $serviceState[$service.Name] = [pscustomobject]@{
        name = $service.Name
        displayName = $service.DisplayName
        pid = $service.Process.Id
        port = $service.Port
        url = $service.Url
        probeUrl = $service.ProbeUrl
        logPath = $service.LogPath
        startedAt = (Get-Date).ToString('s')
    }
}
$state.services = [pscustomobject]$serviceState
Write-LocalState -State $state

Write-Host 'Local services are running.' -ForegroundColor Green
$started | Select-Object DisplayName, @{Name='PID';Expression={$_.Process.Id}}, Port, ProbeUrl, LogPath | Format-Table -AutoSize
Write-Host ''
Write-Host "State File : $(Get-StateFilePath)"
