$ErrorActionPreference = 'Continue'

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir '..\..')).Path
$ScriptsDir = Join-Path $ScriptDir 'scripts'
$MiscDirName = -join ([char[]](0x6742, 0x9879))
$RuntimeDirName = -join ([char[]](0x8FD0, 0x884C, 0x65E5, 0x5FD7))
$RuntimeDir = Join-Path (Join-Path (Join-Path $ProjectRoot $MiscDirName) $RuntimeDirName) 'runtime'
$PublicStatePath = Join-Path $RuntimeDir 'public-tunnel-state.json'
$root = $ProjectRoot
. (Join-Path $ScriptsDir 'local-common.ps1')

$stoppedPublicState = @()
if (Test-Path $PublicStatePath) {
    $state = Get-Content -Path $PublicStatePath -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($process in $state.processes) {
        if ($process.pid) {
            Stop-ProcessTree -RootProcessId ([int]$process.pid) | Out-Null
            $stoppedPublicState += [pscustomobject]@{
                Port = ''
                Pid = [int]$process.pid
                ProcessName = $process.name
            }
            Write-Host "Stopped recorded process: PID=$($process.pid) Name=$($process.name)" -ForegroundColor Green
        }
    }
    Remove-Item -Path $PublicStatePath -Force
}

foreach ($process in (Get-Process cloudflared -ErrorAction SilentlyContinue)) {
    Stop-ProcessTree -RootProcessId $process.Id | Out-Null
    Write-Host "Stopped cloudflared: PID=$($process.Id)" -ForegroundColor Green
}

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
