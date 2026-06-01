param(
    [switch]$NoPublicTunnel,
    [string]$PublicUrl
)

$ErrorActionPreference = "Stop"

# Project root = two levels above this script.
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptDir '..\..')).Path
$BackendDir = Join-Path $ProjectRoot 'backend'
$AdminWebDir = Join-Path $ProjectRoot 'admin-web'
$TraceWebDir = Join-Path $ProjectRoot 'trace-web'
$AdminVitePath = Join-Path $AdminWebDir 'node_modules\vite\bin\vite.js'
$TraceVitePath = Join-Path $TraceWebDir 'node_modules\vite\bin\vite.js'
$ScriptsDir = Join-Path $ScriptDir 'scripts'
$DependencyDirName = -join ([char[]](0x4F9D, 0x8D56, 0x6587, 0x4EF6))
$RedisDir = Join-Path (Join-Path $ScriptDir $DependencyDirName) 'tools\redis'
$MiscDirName = -join ([char[]](0x6742, 0x9879))
$RuntimeDirName = -join ([char[]](0x8FD0, 0x884C, 0x65E5, 0x5FD7))
$RuntimeDir = Join-Path (Join-Path (Join-Path $ProjectRoot $MiscDirName) $RuntimeDirName) 'runtime'
$RuntimeLogDir = Join-Path $RuntimeDir 'logs'
$PublicStatePath = Join-Path $RuntimeDir 'public-tunnel-state.json'
$PublicUrlPath = Join-Path $RuntimeDir 'public-url.txt'
$TunnelOutPath = Join-Path $RuntimeLogDir 'cloudflared-public.out.log'
$TunnelErrPath = Join-Path $RuntimeLogDir 'cloudflared-public.err.log'
$root = $ProjectRoot
. (Join-Path $ScriptsDir 'local-common.ps1')
New-Item -ItemType Directory -Force -Path $RuntimeDir, $RuntimeLogDir | Out-Null

Write-Host "Project root: $root" -ForegroundColor Cyan

function Stop-PortProcess {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        $lines = netstat -ano | Select-String ":$Port\s"

        if (-not $lines) {
            Write-Host "Port $Port is not in use." -ForegroundColor DarkGray
            return
        }

        $processIds = @()

        foreach ($line in $lines) {
            $text = ($line.ToString() -replace "\s+", " ").Trim()
            $parts = $text.Split(" ")
            $processIdText = $parts[-1]

            if ($processIdText -match "^\d+$" -and $processIdText -ne "0") {
                $processIds += [int]$processIdText
            }
        }

        $processIds = $processIds | Sort-Object -Unique

        foreach ($processId in $processIds) {
            try {
                $proc = Get-Process -Id $processId -ErrorAction Stop
                Write-Host "Stopping process on port $Port : PID=$processId Name=$($proc.ProcessName)" -ForegroundColor Yellow
                Stop-Process -Id $processId -Force -ErrorAction Stop
            }
            catch {
                Write-Host "Failed to stop PID=$processId, or it already exited." -ForegroundColor DarkYellow
            }
        }
    }
    catch {
        Write-Host "Failed while checking/stopping port $Port : $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Find-JavaHome {
    $candidates = @(
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Java\jdk-21.0.4",
        "C:\Program Files\Java\jdk-17.0.12",
        "C:\Program Files\Amazon Corretto\jdk21.0.4_7",
        "C:\Program Files\Amazon Corretto\jdk17.0.14_7",
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot"
    )

    foreach ($path in $candidates) {
        if (Test-Path "$path\bin\java.exe") {
            return $path
        }
    }

    $scanRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Amazon Corretto",
        "C:\Program Files\Eclipse Adoptium"
    )

    foreach ($scanRoot in $scanRoots) {
        if (Test-Path $scanRoot) {
            $dirs = Get-ChildItem $scanRoot -Directory -ErrorAction SilentlyContinue
            foreach ($dir in $dirs) {
                if (Test-Path "$($dir.FullName)\bin\java.exe") {
                    return $dir.FullName
                }
            }
        }
    }

    return $null
}

function Start-ServicePowerShell {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [string]$Name = '',
        [string]$LogFileName = ''
    )

    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Command))
    $process = Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand -PassThru
    if ($Name) {
        return [pscustomobject]@{
            name = $Name
            pid = $process.Id
            logPath = if ($LogFileName) { Join-Path $RuntimeLogDir $LogFileName } else { $null }
        }
    }
    return $process
}

function Find-CommandPath {
    param([string[]]$Names)
    foreach ($name in $Names) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($cmd) {
            return $cmd.Source
        }
    }
    return $null
}

function Ensure-Cloudflared {
    $path = Find-CommandPath @('cloudflared.exe', 'cloudflared')
    if ($path) {
        return $path
    }

    $winget = Find-CommandPath @('winget.exe', 'winget')
    if (-not $winget) {
        throw 'cloudflared was not found, and winget was not found either.'
    }

    Write-Host 'cloudflared was not found. Installing it with winget...' -ForegroundColor Yellow
    & $winget install --id Cloudflare.cloudflared --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        throw 'cloudflared installation failed.'
    }

    $path = Find-CommandPath @('cloudflared.exe', 'cloudflared')
    if ($path) {
        return $path
    }

    $knownPath = 'C:\Program Files (x86)\cloudflared\cloudflared.exe'
    if (Test-Path $knownPath) {
        return $knownPath
    }

    throw 'cloudflared is installed, but this shell cannot find it yet. Reopen the terminal and retry.'
}

function Wait-HttpOkLocal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 90
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5 | Out-Null
            return $true
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Wait-TunnelUrl {
    param([string[]]$Paths)
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        foreach ($path in $Paths) {
            if (Test-Path $path) {
                $content = Get-Content -Path $path -Raw -ErrorAction SilentlyContinue
                if ($null -eq $content) {
                    $content = ''
                }
                $match = [regex]::Match($content, 'https://[a-zA-Z0-9-]+\.trycloudflare\.com')
                if ($match.Success) {
                    return $match.Value.TrimEnd('/')
                }
            }
        }
        Start-Sleep -Seconds 1
    }
    throw "Could not read the public URL from cloudflared logs: $($Paths -join ', ')"
}

Write-Host "Checking required ports before startup..." -ForegroundColor Cyan
try {
    $stoppedPortRecords = Resolve-RequiredPortConflicts -Ports @(6379, 8080, 5174, 5173) -AutoStopWorkspaceProcesses
    if ($stoppedPortRecords.Count -gt 0) {
        Write-Host "Stopped existing workspace/demo listeners:" -ForegroundColor Yellow
        $stoppedPortRecords | Select-Object Port, Pid, ProcessName | Format-Table -AutoSize
    }
}
catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Start-Sleep -Seconds 1

$javaHome = Find-JavaHome
if (-not $javaHome) {
    Write-Host "No JDK found. Please install JDK 17 or 21, or edit the paths in start-all.ps1." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Using JDK: $javaHome" -ForegroundColor Green

$redisCmd = @"
`$host.UI.RawUI.WindowTitle = 'Redis'
Set-Location '$RedisDir'
if (!(Test-Path '.\redis-server.exe')) {
    Write-Host 'redis-server.exe not found. Please check the script dependency folder.' -ForegroundColor Red
} else {
    .\redis-server.exe .\redis.windows.conf
}
"@

$backendCmd = @"
`$host.UI.RawUI.WindowTitle = 'Backend'
`$env:JAVA_HOME = '$javaHome'
`$env:Path = '$javaHome\bin;' + `$env:Path
`$env:SPRING_PROFILES_ACTIVE = 'demo'
Set-Location '$BackendDir'
java -version
mvn -version
mvn spring-boot:run
"@

$adminCmd = @"
`$host.UI.RawUI.WindowTitle = 'Admin-Web'
Set-Location '$AdminWebDir'
if (!(Test-Path '.\node_modules')) {
    npm install
}
node '$AdminVitePath' --host 0.0.0.0 --port 5174 --strictPort
"@

$traceCmd = @"
`$host.UI.RawUI.WindowTitle = 'Trace-Web'
Set-Location '$TraceWebDir'
if (!(Test-Path '.\node_modules')) {
    npm install
}
node '$TraceVitePath' --host 0.0.0.0 --port 5173 --strictPort
"@

Write-Host "Starting Redis..." -ForegroundColor Cyan
$startedProcesses = @()
$startedProcesses += Start-ServicePowerShell -Command $redisCmd -Name 'redis'

Start-Sleep -Seconds 2

Write-Host "Starting Trace-Web..." -ForegroundColor Cyan
$startedProcesses += Start-ServicePowerShell -Command $traceCmd -Name 'trace-web' -LogFileName 'trace-web.log'

if (-not (Wait-HttpOkLocal -Url 'http://127.0.0.1:5173' -TimeoutSeconds 90)) {
    Write-Host "Trace-Web did not become ready on http://127.0.0.1:5173." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not $NoPublicTunnel) {
    if ([string]::IsNullOrWhiteSpace($PublicUrl)) {
        Write-Host "Starting Cloudflare public tunnel..." -ForegroundColor Cyan
        $cloudflared = Ensure-Cloudflared
        Set-Content -Path $TunnelOutPath -Value '' -Encoding UTF8
        Set-Content -Path $TunnelErrPath -Value '' -Encoding UTF8
        $tunnelProcess = Start-Process $cloudflared `
            -ArgumentList 'tunnel', '--url', 'http://127.0.0.1:5173' `
            -RedirectStandardOutput $TunnelOutPath `
            -RedirectStandardError $TunnelErrPath `
            -WindowStyle Hidden `
            -PassThru
        $startedProcesses += [pscustomobject]@{
            name = 'cloudflared'
            pid = $tunnelProcess.Id
            logPath = $TunnelOutPath
            errPath = $TunnelErrPath
        }
        $PublicUrl = Wait-TunnelUrl -Paths @($TunnelOutPath, $TunnelErrPath)
    }
    else {
        $PublicUrl = $PublicUrl.TrimEnd('/')
    }

    Set-Content -Path $PublicUrlPath -Value $PublicUrl -Encoding UTF8
    Write-Host "Public Trace URL: $PublicUrl/t/demo-normal-2026" -ForegroundColor Green
    $backendCmd = $backendCmd.Replace("`$env:SPRING_PROFILES_ACTIVE = 'demo'", "`$env:SPRING_PROFILES_ACTIVE = 'demo'`r`n`$env:PUBLIC_TRACE_BASE_URL = '$PublicUrl'`r`n`$env:BACKEND_BASE_URL = '$PublicUrl'")
    $adminCmd = $adminCmd.Replace("Set-Location '$AdminWebDir'", "`$env:VITE_TRACE_WEB_ORIGIN = '$PublicUrl'`r`nSet-Location '$AdminWebDir'")
}

Write-Host "Starting Backend..." -ForegroundColor Cyan
$startedProcesses += Start-ServicePowerShell -Command $backendCmd -Name 'backend' -LogFileName 'backend.log'

Start-Sleep -Seconds 4

Write-Host "Starting Admin-Web..." -ForegroundColor Cyan
$startedProcesses += Start-ServicePowerShell -Command $adminCmd -Name 'admin-web' -LogFileName 'admin-web.log'

Start-Sleep -Seconds 2

$state = [pscustomobject]@{
    createdAt = (Get-Date).ToString('s')
    publicUrl = if ($NoPublicTunnel) { '' } else { $PublicUrl }
    traceDemoUrl = if ($NoPublicTunnel) { 'http://127.0.0.1:5173/t/demo-normal-2026' } else { "$PublicUrl/t/demo-normal-2026" }
    processes = $startedProcesses
}
$state | ConvertTo-Json -Depth 6 | Set-Content -Path $PublicStatePath -Encoding UTF8

Write-Host ""
Write-Host "All start commands have been launched." -ForegroundColor Green
Write-Host "Backend profile -> demo (H2 seeded demo data)" -ForegroundColor Green
Write-Host "Redis     -> 6379"
Write-Host "Backend   -> 8080"
Write-Host "Admin-Web -> 5174"
Write-Host "Trace-Web -> 5173"
Write-Host ""
Write-Host "URLs:" -ForegroundColor Cyan
Write-Host "Swagger   : http://127.0.0.1:8080/swagger-ui.html"
Write-Host "Admin Web : http://127.0.0.1:5174/login"
Write-Host "Trace Web : http://127.0.0.1:5173/t/demo-normal-2026"
if (-not $NoPublicTunnel) {
    Write-Host "Public QR : $PublicUrl/t/demo-normal-2026" -ForegroundColor Green
    Write-Host "Saved URL : $PublicUrlPath"
}
Read-Host "Press Enter to exit"
