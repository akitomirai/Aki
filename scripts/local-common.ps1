$script:ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$script:RuntimeDir = Join-Path $script:ProjectRoot 'output\runtime'
$script:StateFilePath = Join-Path $script:RuntimeDir 'local-run-state.json'
$script:ServiceCatalog = [ordered]@{
    backend = [pscustomobject]@{
        Name = 'backend'
        DisplayName = 'backend'
        Port = 8080
        Url = 'http://127.0.0.1:8080'
        ProbeUrl = 'http://127.0.0.1:8080/actuator/health'
        LogFileName = 'backend.log'
    }
    adminWeb = [pscustomobject]@{
        Name = 'adminWeb'
        DisplayName = 'admin-web'
        Port = 5174
        Url = 'http://127.0.0.1:5174'
        ProbeUrl = 'http://127.0.0.1:5174/login'
        LogFileName = 'admin-web.log'
    }
    traceWeb = [pscustomobject]@{
        Name = 'traceWeb'
        DisplayName = 'trace-web'
        Port = 5173
        Url = 'http://127.0.0.1:5173'
        ProbeUrl = 'http://127.0.0.1:5173/t/orange-202603-d1'
        LogFileName = 'trace-web.log'
    }
}

function Get-ProjectRoot {
    return $script:ProjectRoot
}

function Get-RuntimeDir {
    if (-not (Test-Path $script:RuntimeDir)) {
        New-Item -ItemType Directory -Force -Path $script:RuntimeDir | Out-Null
    }
    return $script:RuntimeDir
}

function Get-StateFilePath {
    Get-RuntimeDir | Out-Null
    return $script:StateFilePath
}

function Get-ServiceCatalog {
    return $script:ServiceCatalog
}

function Get-ServiceConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    return $script:ServiceCatalog[$Name]
}

function Get-ServiceLogPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $config = Get-ServiceConfig -Name $Name
    return Join-Path (Get-RuntimeDir) $config.LogFileName
}

function Read-LocalState {
    $path = Get-StateFilePath
    if (-not (Test-Path $path)) {
        return $null
    }
    return Get-Content $path -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Write-LocalState {
    param(
        [Parameter(Mandatory = $true)]
        $State
    )

    $path = Get-StateFilePath
    $State | ConvertTo-Json -Depth 8 | Set-Content -Path $path -Encoding UTF8
}

function Remove-LocalState {
    $path = Get-StateFilePath
    if (Test-Path $path) {
        Remove-Item -Path $path -Force
    }
}

function Update-StateServiceEntry {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ServiceName,
        [Parameter(Mandatory = $true)]
        [int]$ProcessId,
        [Parameter(Mandatory = $true)]
        [string]$LogPath
    )

    $state = Read-LocalState
    if ($null -eq $state) {
        return $false
    }

    $config = Get-ServiceConfig -Name $ServiceName
    $services = [ordered]@{}
    if ($state.services) {
        foreach ($property in $state.services.PSObject.Properties) {
            $services[$property.Name] = $property.Value
        }
    }

    $services[$ServiceName] = [pscustomobject]@{
        name = $ServiceName
        displayName = $config.DisplayName
        pid = $ProcessId
        port = $config.Port
        url = $config.Url
        probeUrl = $config.ProbeUrl
        logPath = $LogPath
        startedAt = (Get-Date).ToString('s')
    }

    $state.services = [pscustomobject]$services
    Write-LocalState -State $state
    return $true
}

function Find-JavaHome {
    $candidates = @(
        'C:\Program Files\Java\jdk-21',
        'C:\Program Files\Java\jdk-17',
        'C:\Program Files\Java\jdk-21.0.4',
        'C:\Program Files\Java\jdk-17.0.12',
        'C:\Program Files\Amazon Corretto\jdk21.0.4_7',
        'C:\Program Files\Amazon Corretto\jdk17.0.14_7',
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot',
        'C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot'
    )

    foreach ($path in $candidates) {
        if (Test-Path (Join-Path $path 'bin\java.exe')) {
            return $path
        }
    }

    return $null
}

function Get-HttpResult {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        return [pscustomobject]@{
            Ok = $true
            Url = $Url
            StatusCode = [int]$response.StatusCode
            Content = [string]$response.Content
            Error = ''
        }
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode.value__
            }
            catch {
                $statusCode = $null
            }
        }

        return [pscustomobject]@{
            Ok = $false
            Url = $Url
            StatusCode = $statusCode
            Content = ''
            Error = $_.Exception.Message
        }
    }
}

function Wait-HttpResult {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $result = Get-HttpResult -Url $Url
        if ($result.Ok) {
            return $result
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    return $result
}

function Wait-HttpOk {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 30
    )

    return (Wait-HttpResult -Url $Url -TimeoutSeconds $TimeoutSeconds).Ok
}

function Get-ProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        return $null
    }

    $wmi = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue
    $startTime = $null
    try {
        $startTime = $process.StartTime.ToString('s')
    }
    catch {
        $startTime = $null
    }

    return [pscustomobject]@{
        Pid = $ProcessId
        ProcessName = $process.ProcessName
        Path = $process.Path
        StartTime = $startTime
        CommandLine = if ($wmi) { $wmi.CommandLine } else { '' }
        ParentProcessId = if ($wmi) { [int]$wmi.ParentProcessId } else { $null }
    }
}

function Get-PortListenerRecords {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    $records = @()
    foreach ($processId in $connections) {
        $record = Get-ProcessRecord -ProcessId $processId
        if ($record) {
            $records += $record
        }
    }

    return $records
}

function Test-ProjectProcessRecord {
    param(
        [Parameter(Mandatory = $true)]
        $Record
    )

    $needle = $script:ProjectRoot.ToLowerInvariant()
    $commandLine = [string]$Record.CommandLine
    $path = [string]$Record.Path

    if ($commandLine.ToLowerInvariant().Contains($needle)) {
        return $true
    }
    if ($path.ToLowerInvariant().Contains($needle)) {
        return $true
    }
    if ($commandLine -like '*edu.jxust.agritrace.TraceabilityBackendApplication*' -and $commandLine -like '*spring.profiles.active=demo*') {
        return $true
    }
    return $false
}

function Test-ManagedServiceProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ServiceName,
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    $record = Get-ProcessRecord -ProcessId $ProcessId
    if ($null -eq $record) {
        return $false
    }

    $commandLine = [string]$record.CommandLine
    if ($commandLine -notlike '*local-service-runner.ps1*') {
        return $false
    }
    if ($commandLine -notlike "*-Service*${ServiceName}*") {
        return $false
    }

    return $true
}

function Get-ChildProcessIds {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ParentPid
    )

    $childIds = @()
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentPid" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        $childPid = [int]$child.ProcessId
        $childIds += $childPid
        $childIds += Get-ChildProcessIds -ParentPid $childPid
    }

    return $childIds
}

function Stop-ProcessTree {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RootProcessId
    )

    $stopped = @()
    $children = Get-ChildProcessIds -ParentPid $RootProcessId | Select-Object -Unique
    foreach ($childPid in ($children | Sort-Object -Descending)) {
        try {
            Stop-Process -Id $childPid -Force -ErrorAction Stop
            $stopped += $childPid
        }
        catch {
        }
    }

    try {
        Stop-Process -Id $RootProcessId -Force -ErrorAction Stop
        $stopped += $RootProcessId
    }
    catch {
    }

    return $stopped | Select-Object -Unique
}

function Stop-ProjectPortProcess {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $records = Get-PortListenerRecords -Port $Port
    foreach ($record in $records) {
        if (-not (Test-ProjectProcessRecord -Record $record)) {
            throw "Port $Port is occupied by PID $($record.Pid) ($($record.ProcessName)). Stop that process before continuing."
        }
        Stop-ProcessTree -RootProcessId $record.Pid | Out-Null
    }
}

function Get-PortConflictEntries {
    param(
        [Parameter(Mandatory = $true)]
        [int[]]$Ports
    )

    $entries = @()
    foreach ($port in ($Ports | Select-Object -Unique)) {
        $records = Get-PortListenerRecords -Port $port
        foreach ($record in $records) {
            $entries += [pscustomobject]@{
                Port = $port
                Pid = $record.Pid
                ProcessName = $record.ProcessName
                Path = $record.Path
                CommandLine = $record.CommandLine
                ManagedByWorkspace = Test-ProjectProcessRecord -Record $record
            }
        }
    }

    return $entries | Sort-Object Port, Pid -Unique
}

function Format-PortConflictSummary {
    param(
        [Parameter(Mandatory = $true)]
        [array]$Entries
    )

    return ($Entries | ForEach-Object {
        $scope = if ($_.ManagedByWorkspace) { 'workspace/demo process' } else { 'external process' }
        $command = [string]$_.CommandLine
        if ($command.Length -gt 140) {
            $command = $command.Substring(0, 140) + '...'
        }
        "port $($_.Port) -> pid $($_.Pid) ($($_.ProcessName), $scope)`n  $command"
    }) -join [Environment]::NewLine
}

function Resolve-RequiredPortConflicts {
    param(
        [Parameter(Mandatory = $true)]
        [int[]]$Ports,
        [switch]$AutoStopWorkspaceProcesses
    )

    $conflicts = Get-PortConflictEntries -Ports $Ports
    if ($conflicts.Count -eq 0) {
        return @()
    }

    $stopped = @()
    if ($AutoStopWorkspaceProcesses) {
        $workspaceConflicts = $conflicts | Where-Object { $_.ManagedByWorkspace }
        foreach ($entry in ($workspaceConflicts | Sort-Object Pid -Unique)) {
            Stop-ProcessTree -RootProcessId $entry.Pid | Out-Null
            $stopped += $entry
        }

        if ($stopped.Count -gt 0) {
            Start-Sleep -Seconds 1
        }
    }

    $remaining = Get-PortConflictEntries -Ports $Ports
    if ($remaining.Count -gt 0) {
        $summary = Format-PortConflictSummary -Entries $remaining
        throw "Required ports are already in use.`n$summary`nOnly workspace/demo processes are auto-cleaned. Please stop external listeners first."
    }

    return $stopped
}

function Show-LogTail {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [int]$Tail = 40
    )

    if (Test-Path $Path) {
        Get-Content $Path -Encoding UTF8 | Select-Object -Last $Tail
    }
}
