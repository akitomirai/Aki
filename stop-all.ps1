$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $root 'scripts\local-common.ps1')

$ports = @(6379, 8080, 5174, 5173)
$stopped = @()
$skipped = @()

foreach ($port in $ports) {
    $records = Get-PortListenerRecords -Port $port
    if ($records.Count -eq 0) {
        Write-Host "Port $port is not in use." -ForegroundColor DarkGray
        continue
    }

    foreach ($record in $records) {
        if (-not (Test-ProjectProcessRecord -Record $record)) {
            $skipped += [pscustomobject]@{
                Port = $port
                Pid = $record.Pid
                ProcessName = $record.ProcessName
            }
            Write-Host "Skip external listener on port $port : PID=$($record.Pid) Name=$($record.ProcessName)" -ForegroundColor Yellow
            continue
        }

        Stop-ProcessTree -RootProcessId $record.Pid | Out-Null
        $stopped += [pscustomobject]@{
            Port = $port
            Pid = $record.Pid
            ProcessName = $record.ProcessName
        }
        Write-Host "Stopped workspace listener on port $port : PID=$($record.Pid) Name=$($record.ProcessName)" -ForegroundColor Green
    }
}

if ($stopped.Count -eq 0 -and $skipped.Count -eq 0) {
    Write-Host 'No project-related listeners were found.' -ForegroundColor Yellow
}

Read-Host "Press Enter to exit"
