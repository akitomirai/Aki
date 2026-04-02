$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\round8-demo-common.ps1"

$backendBase = 'http://127.0.0.1:8080'
$adminBase = 'http://127.0.0.1:5174'
$traceBase = 'http://127.0.0.1:5173'
$publicTraceToken = 'orange-202603-d1'
$baselineBatchCode = 'ORANGE-202603-D1'
$platformCredentials = @{
    username = 'platform'
    password = '123456'
}
$failures = 0
$platformToken = $null

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

function Get-JsonResponse {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [ValidateSet('Get', 'Post')]
        [string]$Method = 'Get',
        [hashtable]$Headers,
        [object]$Body
    )

    $params = @{
        Uri = $Uri
        Method = $Method
        ErrorAction = 'Stop'
    }

    if ($Headers) {
        $params.Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 5)
    }

    return Invoke-RestMethod @params
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

Write-Step "Checking admin-web login route..."
try {
    Invoke-WebRequest -UseBasicParsing "$adminBase/login" | Out-Null
    Write-Pass "Admin-web login route is reachable on port 5174."
} catch {
    Write-Fail "Admin-web login route is not reachable on port 5174."
}

Write-Step "Checking trace-web public page..."
try {
    Invoke-WebRequest -UseBasicParsing "$traceBase/t/$publicTraceToken" | Out-Null
    Write-Pass "Trace-web public page is reachable on port 5173."
} catch {
    Write-Fail "Trace-web public page is not reachable on port 5173."
}

Write-Step "Checking backend public trace token..."
try {
    $trace = Get-JsonResponse -Uri "$backendBase/api/public/traces/$publicTraceToken"
    if ($trace.success -and $trace.data.summary.batchCode) {
        Write-Pass "Public trace token '$publicTraceToken' is available."
        Write-Host ("  - Product: {0}" -f $trace.data.summary.productName)
        Write-Host ("  - Batch:   {0}" -f $trace.data.summary.batchCode)
    } else {
        Write-Fail "Public trace token check returned no usable data."
    }
} catch {
    Write-Fail "Public trace token '$publicTraceToken' is not reachable."
}

Write-Step "Checking authenticated platform login..."
try {
    $login = Get-JsonResponse -Uri "$backendBase/api/auth/login" -Method Post -Body $platformCredentials
    if ($login.success -and $login.data.token) {
        $platformToken = $login.data.token
        Write-Pass "Platform login succeeded for authenticated backend checks."
        Write-Host ("  - User: {0}" -f $login.data.user.realName)
        Write-Host ("  - Role: {0}" -f $login.data.user.roleCode)
    } else {
        Write-Fail "Platform login did not return a usable token."
    }
} catch {
    Write-Fail "Platform login check failed."
}

Write-Step "Checking authenticated batch baseline..."
if ([string]::IsNullOrWhiteSpace($platformToken)) {
    Write-Fail "Authenticated batch baseline check was skipped because platform login did not return a token."
} else {
    try {
        $batches = Get-JsonResponse -Uri "$backendBase/api/batches" -Headers @{ Authorization = "Bearer $platformToken" }
        $items = @($batches.data)
        $baselineBatch = $items | Where-Object { $_.batchCode -eq $baselineBatchCode } | Select-Object -First 1
        if ($batches.success -and $baselineBatch) {
            Write-Pass "Authenticated admin batch list returned the seeded baseline batch."
            Write-Host ("  - Batch:  {0}" -f $baselineBatch.batchCode)
            Write-Host ("  - Status: {0}" -f $baselineBatch.statusLabel)
        } else {
            Write-Fail "Authenticated batch list did not contain the seeded baseline batch."
        }
    } catch {
        Write-Fail "Authenticated batch list check failed."
    }
}

Write-Step "Quick links"
Write-Host ("  Admin login:      {0}/login" -f $adminBase)
Write-Host ("  Admin workbench:  {0}/batches/2 (requires login)" -f $adminBase)
Write-Host ("  Public trace:     {0}/t/{1}" -f $traceBase, $publicTraceToken)
Write-Host ("  Public trace API: {0}/api/public/traces/{1}" -f $backendBase, $publicTraceToken)

if ($failures -gt 0) {
    exit 1
}
