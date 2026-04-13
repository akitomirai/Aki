$ErrorActionPreference = "Stop"

# Project root = directory of this script
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $root 'scripts\local-common.ps1')

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

        $pids = @()

        foreach ($line in $lines) {
            $text = ($line.ToString() -replace "\s+", " ").Trim()
            $parts = $text.Split(" ")
            $pid = $parts[-1]

            if ($pid -match "^\d+$" -and $pid -ne "0") {
                $pids += [int]$pid
            }
        }

        $pids = $pids | Sort-Object -Unique

        foreach ($pid in $pids) {
            try {
                $proc = Get-Process -Id $pid -ErrorAction Stop
                Write-Host "Stopping process on port $Port : PID=$pid Name=$($proc.ProcessName)" -ForegroundColor Yellow
                Stop-Process -Id $pid -Force -ErrorAction Stop
            }
            catch {
                Write-Host "Failed to stop PID=$pid, or it already exited." -ForegroundColor DarkYellow
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
Set-Location '$root\tools\redis'
if (!(Test-Path '.\redis-server.exe')) {
    Write-Host 'redis-server.exe not found. Please check tools\redis.' -ForegroundColor Red
} else {
    .\redis-server.exe .\redis.windows.conf
}
"@

$backendCmd = @"
`$host.UI.RawUI.WindowTitle = 'Backend'
`$env:JAVA_HOME = '$javaHome'
`$env:Path = '$javaHome\bin;' + `$env:Path
`$env:SPRING_PROFILES_ACTIVE = 'demo'
Set-Location '$root\backend'
java -version
mvn -version
mvn spring-boot:run
"@

$adminCmd = @"
`$host.UI.RawUI.WindowTitle = 'Admin-Web'
Set-Location '$root\admin-web'
if (!(Test-Path '.\node_modules')) {
    npm install
}
npm run dev
"@

$traceCmd = @"
`$host.UI.RawUI.WindowTitle = 'Trace-Web'
Set-Location '$root\trace-web'
if (!(Test-Path '.\node_modules')) {
    npm install
}
npm run dev
"@

Write-Host "Starting Redis..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $redisCmd

Start-Sleep -Seconds 2

Write-Host "Starting Backend..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $backendCmd

Start-Sleep -Seconds 4

Write-Host "Starting Admin-Web..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $adminCmd

Start-Sleep -Seconds 2

Write-Host "Starting Trace-Web..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $traceCmd

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
Read-Host "Press Enter to exit"
