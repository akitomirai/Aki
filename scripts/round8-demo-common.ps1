$ErrorActionPreference = 'Stop'

$script:Round8Root = Split-Path -Parent $PSScriptRoot
$script:Round8ArtifactsDir = Join-Path $script:Round8Root 'artifacts'
$script:Round8LogsDir = Join-Path $script:Round8ArtifactsDir 'logs'
$script:Round8ServiceLogsDir = Join-Path $script:Round8LogsDir 'services'
$script:Round8ReportDir = Join-Path $script:Round8ArtifactsDir 'playwright-report'
$script:Round8TestResultsDir = Join-Path $script:Round8ArtifactsDir 'test-results'
$script:Round8ScreenshotsDir = Join-Path $script:Round8ArtifactsDir 'screenshots'
$script:Round8TracesDir = Join-Path $script:Round8ArtifactsDir 'traces'
$script:Round8VideosDir = Join-Path $script:Round8ArtifactsDir 'videos'
$script:Round8StateFile = Join-Path $script:Round8LogsDir 'round8-processes.json'

function Get-Round8Root {
    return $script:Round8Root
}

function Get-Round8Paths {
    return [ordered]@{
        Root = $script:Round8Root
        Artifacts = $script:Round8ArtifactsDir
        Logs = $script:Round8LogsDir
        ServiceLogs = $script:Round8ServiceLogsDir
        Report = $script:Round8ReportDir
        TestResults = $script:Round8TestResultsDir
        Screenshots = $script:Round8ScreenshotsDir
        Traces = $script:Round8TracesDir
        Videos = $script:Round8VideosDir
        StateFile = $script:Round8StateFile
    }
}

function Ensure-Round8Directories {
    $paths = Get-Round8Paths
    @(
        $paths.Artifacts,
        $paths.Logs,
        $paths.ServiceLogs,
        $paths.Report,
        $paths.TestResults,
        $paths.Screenshots,
        $paths.Traces,
        $paths.Videos
    ) | ForEach-Object {
        New-Item -ItemType Directory -Force -Path $_ | Out-Null
    }
}

function Remove-Round8ServiceLogs {
    $paths = Get-Round8Paths
    if (Test-Path $paths.ServiceLogs) {
        Get-ChildItem $paths.ServiceLogs -File -ErrorAction SilentlyContinue | Remove-Item -Force
    }
}

function Remove-Round8TestArtifacts {
    $paths = Get-Round8Paths
    @(
        $paths.Report,
        $paths.TestResults,
        $paths.Screenshots,
        $paths.Traces,
        $paths.Videos
    ) | ForEach-Object {
        if (Test-Path $_) {
            Remove-Item $_ -Recurse -Force
        }
        New-Item -ItemType Directory -Force -Path $_ | Out-Null
    }

    $jsonReport = Join-Path $paths.Logs 'playwright-results.json'
    if (Test-Path $jsonReport) {
        Remove-Item $jsonReport -Force
    }
}

function Read-Round8State {
    $paths = Get-Round8Paths
    if (-not (Test-Path $paths.StateFile)) {
        return @()
    }
    $raw = Get-Content $paths.StateFile -Raw
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return @()
    }
    $parsed = $raw | ConvertFrom-Json
    if ($parsed -is [System.Array]) {
        return $parsed
    }
    return @($parsed)
}

function Write-Round8State {
    param(
        [Parameter(Mandatory)]
        [array]$Services
    )
    $paths = Get-Round8Paths
    Ensure-Round8Directories
    $Services | ConvertTo-Json -Depth 6 | Set-Content -Path $paths.StateFile -Encoding UTF8
}

function Clear-Round8State {
    $paths = Get-Round8Paths
    if (Test-Path $paths.StateFile) {
        Remove-Item $paths.StateFile -Force
    }
}

function Get-ListeningPids {
    param(
        [int[]]$Ports
    )
    return Get-NetTCPConnection -LocalPort $Ports -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
}

function Get-PortUsageSummary {
    param(
        [int[]]$Ports
    )
    return Get-NetTCPConnection -LocalPort $Ports -State Listen -ErrorAction SilentlyContinue |
        Select-Object LocalPort, OwningProcess
}

function Stop-Round8Processes {
    param(
        [switch]$Quiet
    )

    $tracked = Read-Round8State
    foreach ($service in $tracked) {
        if ($service.pid) {
            try {
                Stop-Process -Id $service.pid -Force -ErrorAction Stop
                if (-not $Quiet) {
                    Write-Host ("Stopped tracked process {0} ({1})." -f $service.name, $service.pid)
                }
            } catch {
            }
        }
    }

    Start-Sleep -Milliseconds 800

    $leftovers = Get-ListeningPids -Ports @(8080, 5173, 5174)
    foreach ($processId in $leftovers) {
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            if (-not $Quiet) {
                Write-Host ("Stopped leftover process on demo ports: {0}" -f $processId)
            }
        } catch {
        }
    }

    Clear-Round8State
}

function Start-Round8Process {
    param(
        [Parameter(Mandatory)]
        [string]$Name,
        [Parameter(Mandatory)]
        [string]$Command,
        [Parameter(Mandatory)]
        [string]$LogFile,
        [Parameter(Mandatory)]
        [int]$Port,
        [Parameter(Mandatory)]
        [string]$ReadyUrl
    )

    if (Test-Path $LogFile) {
        Remove-Item $LogFile -Force
    }
    $process = Start-Process cmd.exe -ArgumentList '/c', $Command -PassThru
    return [ordered]@{
        name = $Name
        pid = $process.Id
        logFile = $LogFile
        port = $Port
        readyUrl = $ReadyUrl
    }
}

function Wait-ForRound8Url {
    param(
        [Parameter(Mandatory)]
        [string]$Url,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -UseBasicParsing $Url | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Get-LogTail {
    param(
        [Parameter(Mandatory)]
        [string]$LogFile,
        [int]$Tail = 30
    )

    if (-not (Test-Path $LogFile)) {
        return "<log file not found>"
    }
    return (Get-Content $LogFile -Tail $Tail) -join [Environment]::NewLine
}

function Copy-Round8FailureArtifacts {
    $paths = Get-Round8Paths

    Get-ChildItem $paths.TestResults -Recurse -File -ErrorAction SilentlyContinue | ForEach-Object {
        switch ($_.Extension.ToLowerInvariant()) {
            '.png' {
                Copy-Item $_.FullName (Join-Path $paths.Screenshots $_.Name) -Force
            }
            '.zip' {
                Copy-Item $_.FullName (Join-Path $paths.Traces $_.Name) -Force
            }
            '.webm' {
                Copy-Item $_.FullName (Join-Path $paths.Videos $_.Name) -Force
            }
        }
    }
}
