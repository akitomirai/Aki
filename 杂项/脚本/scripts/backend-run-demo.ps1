[CmdletBinding()]
param(
    [switch]$ForceRestart,
    [int]$StartupTimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'

$envInfo = . (Join-Path $PSScriptRoot 'backend-env.ps1') -Quiet
$projectRoot = $envInfo.ProjectRoot
$backendDir = $envInfo.BackendDir
$miscDirName = -join ([char[]](0x6742, 0x9879))
$logDirName = -join ([char[]](0x65E5, 0x5FD7))
$runtimeDir = Join-Path (Join-Path (Join-Path $projectRoot $miscDirName) $logDirName) 'runtime'
$jarPath = Join-Path $backendDir 'target\traceability-backend-0.0.1-SNAPSHOT.jar'
$healthUrl = 'http://127.0.0.1:8080/actuator/health'
$stdoutLogPath = Join-Path $runtimeDir 'backend-demo.stdout.log'
$stderrLogPath = Join-Path $runtimeDir 'backend-demo.stderr.log'
$statePath = Join-Path $runtimeDir 'backend-demo-state.json'

function Get-ListeningProcessIds {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($null -eq $connections) {
        return @()
    }

    return @($connections | Select-Object -ExpandProperty OwningProcess -Unique)
}

function Get-ProcessCommandLine {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return ''
    }

    return [string]$process.CommandLine
}

function Read-DemoState {
    if (-not (Test-Path $statePath)) {
        return $null
    }

    return Get-Content $statePath -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Write-DemoState {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $state = [pscustomobject]@{
        pid = $ProcessId
        port = 8080
        healthUrl = $healthUrl
        stdoutLogPath = $stdoutLogPath
        stderrLogPath = $stderrLogPath
        jarPath = $jarPath
        startedAt = (Get-Date).ToString('s')
    }

    $state | ConvertTo-Json -Depth 4 | Set-Content -Path $statePath -Encoding UTF8
}

function Remove-DemoState {
    if (Test-Path $statePath) {
        Remove-Item -Path $statePath -Force
    }
}

function Stop-TrackedProcess {
    $state = Read-DemoState
    if ($null -eq $state -or $null -eq $state.pid) {
        return
    }

    $process = Get-Process -Id ([int]$state.pid) -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        Stop-Process -Id $process.Id -Force
        Start-Sleep -Seconds 2
    }

    Remove-DemoState
}

function Stop-CompatibleListeners {
    param(
        [Parameter(Mandatory = $true)]
        [int[]]$ProcessIds
    )

    $normalizedJarPath = $jarPath.ToLowerInvariant()
    $normalizedBackendDir = $backendDir.ToLowerInvariant()

    foreach ($processId in $ProcessIds) {
        $commandLine = (Get-ProcessCommandLine -ProcessId $processId).ToLowerInvariant()
        $isTrackedState = $false

        $state = Read-DemoState
        if ($null -ne $state -and $state.pid -eq $processId) {
            $isTrackedState = $true
        }

        $looksLikeWorkspaceBackend = $commandLine.Contains($normalizedJarPath) -or $commandLine.Contains($normalizedBackendDir)
        if (-not $isTrackedState -and -not $looksLikeWorkspaceBackend) {
            return $false
        }
    }

    foreach ($processId in $ProcessIds) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($null -ne $process) {
            Stop-Process -Id $processId -Force
        }
    }

    Start-Sleep -Seconds 2
    Remove-DemoState
    return $true
}

function Wait-HealthUp {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId,
        [Parameter(Mandatory = $true)]
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            return $false
        }

        try {
            $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 3
            if ($response.status -eq 'UP') {
                return $true
            }
        }
        catch {
        }

        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    return $false
}

if (-not (Test-Path $runtimeDir)) {
    New-Item -ItemType Directory -Force -Path $runtimeDir | Out-Null
}

$listeners = Get-ListeningProcessIds -Port 8080
if ($listeners.Count -gt 0) {
    if (-not $ForceRestart) {
        throw "Port 8080 is already in use by PID(s): $($listeners -join ', '). Use -ForceRestart to replace the tracked demo backend."
    }

    Stop-TrackedProcess
    $listeners = Get-ListeningProcessIds -Port 8080
    if ($listeners.Count -gt 0) {
        if (-not (Stop-CompatibleListeners -ProcessIds $listeners)) {
            throw "Port 8080 is still in use by PID(s): $($listeners -join ', '). Refusing to kill an unknown process."
        }

        $listeners = Get-ListeningProcessIds -Port 8080
        if ($listeners.Count -gt 0) {
            throw "Port 8080 is still in use by PID(s): $($listeners -join ', ') after restart cleanup."
        }
    }
}

if (-not (Test-Path $jarPath)) {
    Write-Output 'Demo jar was not found. Running package build first.'
    Push-Location $backendDir
    try {
        & mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw 'Failed to build demo jar.'
        }
    }
    finally {
        Pop-Location
    }
}

Set-Content -Path $stdoutLogPath -Value '' -Encoding UTF8
Set-Content -Path $stderrLogPath -Value '' -Encoding UTF8

$process = Start-Process `
    -FilePath (Join-Path $env:JAVA_HOME 'bin\java.exe') `
    -ArgumentList @('-jar', $jarPath, '--spring.profiles.active=demo') `
    -WorkingDirectory $backendDir `
    -RedirectStandardOutput $stdoutLogPath `
    -RedirectStandardError $stderrLogPath `
    -WindowStyle Hidden `
    -PassThru

if (-not (Wait-HealthUp -ProcessId $process.Id -TimeoutSeconds $StartupTimeoutSeconds)) {
    Remove-DemoState
    $stdoutTail = if (Test-Path $stdoutLogPath) { Get-Content $stdoutLogPath -Tail 40 } else { @() }
    $stderrTail = if (Test-Path $stderrLogPath) { Get-Content $stderrLogPath -Tail 40 } else { @() }
    throw @(
        "Demo backend failed to become healthy within $StartupTimeoutSeconds seconds."
        "Health URL : $healthUrl"
        "Stdout Log : $stdoutLogPath"
        "Stderr Log : $stderrLogPath"
        'Last stdout lines:'
        ($stdoutTail -join [Environment]::NewLine)
        'Last stderr lines:'
        ($stderrTail -join [Environment]::NewLine)
    ) -join [Environment]::NewLine
}

Write-DemoState -ProcessId $process.Id

Write-Output "Demo backend started."
Write-Output "PID        : $($process.Id)"
Write-Output "Health URL : $healthUrl"
Write-Output "Stdout Log : $stdoutLogPath"
Write-Output "Stderr Log : $stderrLogPath"
Write-Output "State File : $statePath"
