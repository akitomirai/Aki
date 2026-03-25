$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\round8-demo-common.ps1"

$backendBase = 'http://127.0.0.1:8080'
$adminBase = 'http://127.0.0.1:5174'
$traceBase = 'http://127.0.0.1:5173'
$demoToken = 'demo-normal-2026'
$failures = 0

function Write-Step($text) {
    Write-Host ""
    Write-Host $text -ForegroundColor Cyan
}

function Write-Pass($text) {
    Write-Host "[PASS] $text" -ForegroundColor Green
}

function Write-Fail($text) {
    $script:failures += 1
    Write-Host "[FAIL] $text" -ForegroundColor Red
}

Write-Step "Checking backend health..."
try {
    $health = Invoke-RestMethod -Uri "$backendBase/actuator/health" -Method Get -ErrorAction Stop
    if ($health.status -eq 'UP') {
        Write-Pass "Backend is running on port 8080."
    } else {
        Write-Fail "Backend responded, but health status is $($health.status)."
    }
} catch {
    Write-Fail "Backend is not reachable. Start backend with demo profile first."
}

Write-Step "Checking admin-web..."
try {
    Invoke-WebRequest -UseBasicParsing "$adminBase/batches" | Out-Null
    Write-Pass "Admin-web is reachable on port 5174."
} catch {
    Write-Fail "Admin-web is not reachable on port 5174."
}

Write-Step "Checking trace-web..."
try {
    Invoke-WebRequest -UseBasicParsing "$traceBase/t/$demoToken" | Out-Null
    Write-Pass "Trace-web is reachable on port 5173."
} catch {
    Write-Fail "Trace-web is not reachable on port 5173."
}

Write-Step "Checking demo batch data..."
try {
    $batches = Invoke-RestMethod -Uri "$backendBase/api/batches" -Method Get -ErrorAction Stop
    if ($batches.success -and $batches.data.Count -ge 3) {
        Write-Pass "Demo batches are available."
        $batches.data | Select-Object -First 4 | ForEach-Object {
            Write-Host ("  - {0} | {1} | {2}" -f $_.batchCode, $_.productName, $_.statusLabel)
        }
    } else {
        Write-Fail "Batch list returned no usable demo data."
    }
} catch {
    Write-Fail "Batch list check failed."
}

Write-Step "Checking public trace token..."
try {
    $trace = Invoke-RestMethod -Uri "$backendBase/api/public/traces/$demoToken" -Method Get -ErrorAction Stop
    if ($trace.success -and $trace.data.summary.batchCode) {
        Write-Pass "Demo token '$demoToken' is available for public trace preview."
        Write-Host ("  - Product: {0}" -f $trace.data.summary.productName)
        Write-Host ("  - Batch:   {0}" -f $trace.data.summary.batchCode)
    } else {
        Write-Fail "Public trace token check returned no usable data."
    }
} catch {
    Write-Fail "Public trace token '$demoToken' is not reachable."
}

Write-Step "Quick links"
Write-Host ("  Admin: {0}/batches" -f $adminBase)
Write-Host ("  Trace: {0}/t/{1}" -f $traceBase, $demoToken)
Write-Host ("  Token: {0}" -f $demoToken)

if ($failures -gt 0) {
    exit 1
}
