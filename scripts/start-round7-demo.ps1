$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $root 'output\demo-run'
$javaHome = 'C:\Program Files\Java\jdk-21'

New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw "JDK 21 was not found at $javaHome."
}

function Start-DemoProcess {
    param(
        [string]$Name,
        [string]$Command,
        [string]$LogFile
    )
    if (Test-Path $LogFile) {
        Remove-Item $LogFile -Force
    }
    $process = Start-Process cmd.exe -ArgumentList '/c', $Command -PassThru
    [PSCustomObject]@{
        Name = $Name
        Id = $process.Id
        Log = $LogFile
    }
}

function Wait-ForUrl {
    param(
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

$backend = Start-DemoProcess `
    -Name 'backend-demo' `
    -Command ("set ""JAVA_HOME={0}"" && set ""PATH=%JAVA_HOME%\bin;%PATH%"" && cd /d ""{1}"" && mvn spring-boot:run ""-Dspring-boot.run.profiles=demo"" > ""{2}"" 2>&1" -f $javaHome, (Join-Path $root 'backend'), (Join-Path $logs 'backend-demo.log')) `
    -LogFile (Join-Path $logs 'backend-demo.log')

$admin = Start-DemoProcess `
    -Name 'admin-web' `
    -Command ("cd /d ""{0}"" && npx vite --host 127.0.0.1 --port 5174 > ""{1}"" 2>&1" -f (Join-Path $root 'admin-web'), (Join-Path $logs 'admin-web.log')) `
    -LogFile (Join-Path $logs 'admin-web.log')

$trace = Start-DemoProcess `
    -Name 'trace-web' `
    -Command ("cd /d ""{0}"" && npx vite --host 127.0.0.1 --port 5173 > ""{1}"" 2>&1" -f (Join-Path $root 'trace-web'), (Join-Path $logs 'trace-web.log')) `
    -LogFile (Join-Path $logs 'trace-web.log')

$backendReady = Wait-ForUrl -Url 'http://127.0.0.1:8080/actuator/health' -TimeoutSeconds 90
$adminReady = Wait-ForUrl -Url 'http://127.0.0.1:5174' -TimeoutSeconds 60
$traceReady = Wait-ForUrl -Url 'http://127.0.0.1:5173' -TimeoutSeconds 60

Write-Host ""
Write-Host "Round 7 demo processes started." -ForegroundColor Green
Write-Host ("  backend-demo PID: {0}" -f $backend.Id)
Write-Host ("  admin-web PID:    {0}" -f $admin.Id)
Write-Host ("  trace-web PID:    {0}" -f $trace.Id)
Write-Host ""
Write-Host ("  backend ready: {0}" -f $backendReady)
Write-Host ("  admin ready:   {0}" -f $adminReady)
Write-Host ("  trace ready:   {0}" -f $traceReady)
Write-Host ""
Write-Host "Demo URLs"
Write-Host "  Admin list:   http://127.0.0.1:5174/batches"
Write-Host "  Public trace: http://127.0.0.1:5173/t/demo-normal-2026"
Write-Host ""
Write-Host "Recommended demo batches"
Write-Host "  DEMO-ORANGE-202603-A1  (normal published batch)"
Write-Host "  DEMO-ORANGE-202603-D1  (draft batch for admin flow)"
Write-Host "  DEMO-TEA-202603-F1     (frozen batch for handling demo)"
Write-Host "  DEMO-RICE-202603-R1    (recalled batch for regulator demo)"
Write-Host ""
Write-Host "Log files"
Write-Host ("  {0}" -f $backend.Log)
Write-Host ("  {0}" -f $admin.Log)
Write-Host ("  {0}" -f $trace.Log)
