$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'local-common.ps1')

$state = Read-LocalState
$checks = @(
    [pscustomobject]@{
        Name = 'backend'
        Label = 'backend'
        Url = 'http://127.0.0.1:8080/actuator/health'
    }
    [pscustomobject]@{
        Name = 'adminWeb'
        Label = 'admin-web'
        Url = 'http://127.0.0.1:5174/login'
    }
    [pscustomobject]@{
        Name = 'traceWeb'
        Label = 'trace-web'
        Url = 'http://127.0.0.1:5173/t/orange-202603-d1'
    }
)

$results = @()
foreach ($check in $checks) {
    $result = Get-HttpResult -Url $check.Url
    $entry = $null
    if ($state -and $state.services) {
        $entry = $state.services.$($check.Name)
    }

    $detail = ''
    if ($check.Name -eq 'backend' -and $result.Ok) {
        if ($result.Content -match '"status"\s*:\s*"UP"') {
            $detail = 'health status UP'
        }
        else {
            $detail = 'health endpoint responded but UP was not found'
        }
    }
    elseif ($check.Name -eq 'adminWeb' -and $result.Ok) {
        $detail = 'login route reachable'
    }
    elseif ($check.Name -eq 'traceWeb' -and $result.Ok) {
        $detail = 'public trace route reachable'
    }
    else {
        $detail = $result.Error
    }

    $results += [pscustomobject]@{
        Service = $check.Label
        Status = if ($result.Ok) { 'success' } else { 'failed' }
        StatusCode = if ($null -ne $result.StatusCode) { $result.StatusCode } else { '-' }
        Url = $check.Url
        Pid = if ($entry) { $entry.pid } else { '-' }
        LogPath = if ($entry) { $entry.logPath } else { '-' }
        Detail = $detail
    }
}

Write-Host 'Local health check:' -ForegroundColor Cyan
$results | Format-Table -AutoSize

if (($results | Where-Object { $_.Status -eq 'failed' }).Count -gt 0) {
    exit 1
}
