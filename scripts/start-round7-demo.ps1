param(
    [switch]$NoWait
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\round8-demo-common.ps1"

$javaHome = 'C:\Program Files\Java\jdk-21'
$paths = Get-Round8Paths

Ensure-Round8Directories

if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw "JDK 21 was not found at $javaHome."
}

$occupied = Get-PortUsageSummary -Ports @(8080, 5173, 5174)
if ($occupied) {
    Write-Host "Detected existing listeners on demo ports. Cleaning them before restart..." -ForegroundColor Yellow
    $occupied | ForEach-Object {
        Write-Host ("  port {0} -> pid {1}" -f $_.LocalPort, $_.OwningProcess) -ForegroundColor Yellow
    }
    Stop-Round8Processes -Quiet
    Start-Sleep -Seconds 1
}

$stillOccupied = Get-PortUsageSummary -Ports @(8080, 5173, 5174)
if ($stillOccupied) {
    Write-Host "Ports are still occupied after cleanup:" -ForegroundColor Red
    $stillOccupied | ForEach-Object {
        Write-Host ("  port {0} -> pid {1}" -f $_.LocalPort, $_.OwningProcess) -ForegroundColor Red
    }
    throw "Could not free demo ports 8080/5173/5174."
}

Remove-Round8ServiceLogs

$backend = Start-Round8Process `
    -Name 'backend-demo' `
    -Command ("set ""JAVA_HOME={0}"" && set ""PATH=%JAVA_HOME%\bin;%PATH%"" && cd /d ""{1}"" && mvn spring-boot:run ""-Dspring-boot.run.profiles=demo"" > ""{2}"" 2>&1" -f $javaHome, (Join-Path (Get-Round8Root) 'backend'), (Join-Path $paths.ServiceLogs 'backend-demo.log')) `
    -LogFile (Join-Path $paths.ServiceLogs 'backend-demo.log') `
    -Port 8080 `
    -ReadyUrl 'http://127.0.0.1:8080/actuator/health'

$admin = Start-Round8Process `
    -Name 'admin-web' `
    -Command ("cd /d ""{0}"" && npx vite --host 127.0.0.1 --port 5174 > ""{1}"" 2>&1" -f (Join-Path (Get-Round8Root) 'admin-web'), (Join-Path $paths.ServiceLogs 'admin-web.log')) `
    -LogFile (Join-Path $paths.ServiceLogs 'admin-web.log') `
    -Port 5174 `
    -ReadyUrl 'http://127.0.0.1:5174/batches'

$trace = Start-Round8Process `
    -Name 'trace-web' `
    -Command ("cd /d ""{0}"" && npx vite --host 127.0.0.1 --port 5173 > ""{1}"" 2>&1" -f (Join-Path (Get-Round8Root) 'trace-web'), (Join-Path $paths.ServiceLogs 'trace-web.log')) `
    -LogFile (Join-Path $paths.ServiceLogs 'trace-web.log') `
    -Port 5173 `
    -ReadyUrl 'http://127.0.0.1:5173/t/demo-normal-2026'

$services = @($backend, $admin, $trace)
Write-Round8State -Services $services

if ($NoWait) {
    Write-Host "Round 8 demo services were started without readiness wait." -ForegroundColor Green
    return
}

$failed = @()
foreach ($service in $services) {
    $timeout = if ($service.name -eq 'backend-demo') { 120 } else { 75 }
    $ready = Wait-ForRound8Url -Url $service.readyUrl -TimeoutSeconds $timeout
    if (-not $ready) {
        $running = Get-Process -Id $service.pid -ErrorAction SilentlyContinue
        $failed += [PSCustomObject]@{
            name = $service.name
            pid = $service.pid
            logFile = $service.logFile
            running = [bool]$running
            tail = Get-LogTail -LogFile $service.logFile -Tail 30
        }
    }
}

if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Round 8 demo startup failed." -ForegroundColor Red
    foreach ($item in $failed) {
        Write-Host ("[{0}] pid={1} running={2}" -f $item.name, $item.pid, $item.running) -ForegroundColor Red
        Write-Host ("log: {0}" -f $item.logFile) -ForegroundColor Yellow
        Write-Host $item.tail
        Write-Host ""
    }
    Stop-Round8Processes -Quiet
    throw "One or more demo services did not become ready."
}

Write-Host ""
Write-Host "Round 8 demo processes started and are ready." -ForegroundColor Green
Write-Host ("  backend-demo PID: {0}" -f $backend.pid)
Write-Host ("  admin-web PID:    {0}" -f $admin.pid)
Write-Host ("  trace-web PID:    {0}" -f $trace.pid)
Write-Host ""
Write-Host "Ready URLs"
Write-Host "  Backend: http://127.0.0.1:8080/actuator/health"
Write-Host "  Admin:   http://127.0.0.1:5174/batches"
Write-Host "  Trace:   http://127.0.0.1:5173/t/demo-normal-2026"
Write-Host ""
Write-Host "Tracked state"
Write-Host ("  {0}" -f $paths.StateFile)
Write-Host ""
Write-Host "Service logs"
Write-Host ("  {0}" -f $backend.logFile)
Write-Host ("  {0}" -f $admin.logFile)
Write-Host ("  {0}" -f $trace.logFile)
