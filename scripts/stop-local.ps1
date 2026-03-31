$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$serviceOrder = @('backend', 'adminWeb', 'traceWeb')
$state = Read-LocalState
$entries = @()

foreach ($serviceName in $serviceOrder) {
    $config = Get-ServiceConfig -Name $serviceName
    $entry = $null
    if ($state -and $state.services) {
        $entry = $state.services.$serviceName
    }

    if ($entry) {
        $entries += [pscustomobject]@{
            Name = $serviceName
            DisplayName = $config.DisplayName
            Port = $config.Port
            Pid = [int]$entry.pid
            LogPath = $entry.logPath
            Source = 'state'
        }
        continue
    }

    $listeners = Get-PortListenerRecords -Port $config.Port | Where-Object {
        Test-ManagedServiceProcess -ServiceName $serviceName -ProcessId $_.Pid
    }
    foreach ($listener in $listeners) {
        $entries += [pscustomobject]@{
            Name = $serviceName
            DisplayName = $config.DisplayName
            Port = $config.Port
            Pid = $listener.Pid
            LogPath = Get-ServiceLogPath -Name $serviceName
            Source = 'port-scan'
        }
    }
}

if ($entries.Count -eq 0) {
    Write-Host 'No managed local services were found.' -ForegroundColor Yellow
    Remove-LocalState
    exit 0
}

$results = @()
foreach ($entry in $entries) {
    $verified = Test-ManagedServiceProcess -ServiceName $entry.Name -ProcessId $entry.Pid
    if (-not $verified) {
        $results += [pscustomobject]@{
            Service = $entry.DisplayName
            Port = $entry.Port
            Pid = $entry.Pid
            Result = 'skipped'
            Note = 'PID no longer matches a managed local runner'
        }
        continue
    }

    $stoppedIds = Stop-ProcessTree -RootProcessId $entry.Pid
    Start-Sleep -Seconds 1
    $stillListening = (Get-PortListenerRecords -Port $entry.Port).Count -gt 0
    $results += [pscustomobject]@{
        Service = $entry.DisplayName
        Port = $entry.Port
        Pid = $entry.Pid
        Result = if ($stillListening) { 'failed' } else { 'stopped' }
        Note = if ($stillListening) { 'port still listening after stop request' } else { "stopped ids: $($stoppedIds -join ', ')" }
    }
}

$remainingEntries = @()
foreach ($entry in $entries) {
    if ((Get-PortListenerRecords -Port $entry.Port).Count -gt 0) {
        $config = Get-ServiceConfig -Name $entry.Name
        $remainingEntries += [pscustomobject]@{
            name = $entry.Name
            displayName = $config.DisplayName
            pid = $entry.Pid
            port = $config.Port
            url = $config.Url
            probeUrl = $config.ProbeUrl
            logPath = $entry.LogPath
            startedAt = (Get-Date).ToString('s')
        }
    }
}

if ($remainingEntries.Count -eq 0) {
    Remove-LocalState
}
else {
    $services = [ordered]@{}
    foreach ($entry in $remainingEntries) {
        $services[$entry.name] = $entry
    }
    Write-LocalState -State ([pscustomobject]@{
        createdAt = (Get-Date).ToString('s')
        projectRoot = Get-ProjectRoot
        runtimeDir = Get-RuntimeDir
        services = [pscustomobject]$services
    })
}

Write-Host 'Stop results:' -ForegroundColor Cyan
$results | Format-Table -AutoSize

if (($results | Where-Object { $_.Result -eq 'failed' }).Count -gt 0) {
    exit 1
}
