$ErrorActionPreference = 'Stop'

$backendBase = 'http://127.0.0.1:8080'
$adminBase = 'http://127.0.0.1:5174'
$traceBase = 'http://127.0.0.1:5173'

$starBatchCode = 'ORANGE-202603-D1'
$pendingBatchCode = 'ORANGE-202604-Q1'
$riskBatchCode = 'TEA-202603-F1'
$recallBatchCode = 'RICE-202603-R1'
$publicTraceToken = 'orange-202603-d1'
$riskTraceToken = 'demo-recall-2026'

$accountChecks = @(
    [pscustomobject]@{
        Username = 'platform'
        Password = '123456'
        RoleCode = 'PLATFORM_ADMIN'
        CompanyId = $null
        DefaultRoute = '/dashboard'
    }
    [pscustomobject]@{
        Username = 'enterprise_admin'
        Password = '123456'
        RoleCode = 'ENTERPRISE_ADMIN'
        CompanyId = 1
        DefaultRoute = '/batches'
    }
    [pscustomobject]@{
        Username = 'operator'
        Password = '123456'
        RoleCode = 'OPERATOR'
        CompanyId = 1
        DefaultRoute = '/field-entry'
    }
    [pscustomobject]@{
        Username = 'regulator'
        Password = '123456'
        RoleCode = 'REGULATOR'
        CompanyId = $null
        DefaultRoute = '/risk'
    }
)

$failures = 0
$platformToken = $null

function Write-Step($text) {
    Write-Host ''
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
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }

    return Invoke-RestMethod @params
}

function Test-HttpUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [string]$SuccessMessage,
        [string]$FailureMessage
    )

    try {
        Invoke-WebRequest -UseBasicParsing $Uri | Out-Null
        Write-Pass $SuccessMessage
    }
    catch {
        Write-Fail $FailureMessage
    }
}

function Test-AccountLogin {
    param(
        [Parameter(Mandatory = $true)]
        $Account
    )

    try {
        $login = Get-JsonResponse -Uri "$backendBase/api/auth/login" -Method Post -Body @{
            username = $Account.Username
            password = $Account.Password
        }

        if (-not $login.success -or -not $login.data.token) {
            Write-Fail "Account $($Account.Username) did not return a usable token."
            return $null
        }

        $user = $login.data.user
        if ($user.roleCode -ne $Account.RoleCode) {
            Write-Fail "Account $($Account.Username) logged in, but roleCode was $($user.roleCode) instead of $($Account.RoleCode)."
            return $null
        }

        if ($null -ne $Account.CompanyId -and [int]$user.companyId -ne [int]$Account.CompanyId) {
            Write-Fail "Account $($Account.Username) logged in, but companyId was $($user.companyId) instead of $($Account.CompanyId)."
            return $null
        }

        Write-Pass ("Account {0} login succeeded ({1}, default route {2})." -f $Account.Username, $Account.RoleCode, $Account.DefaultRoute)
        return $login.data.token
    }
    catch {
        Write-Fail "Account $($Account.Username) login check failed."
        return $null
    }
}

Write-Step 'Checking backend health...'
try {
    $health = Invoke-RestMethod -Uri "$backendBase/actuator/health" -Method Get -ErrorAction Stop
    if ($health.status -eq 'UP') {
        Write-Pass 'Backend is running on port 8080.'
    }
    else {
        Write-Fail "Backend responded, but health status is $($health.status)."
    }
}
catch {
    Write-Fail 'Backend is not reachable. Start backend with demo profile first.'
}

Write-Step 'Checking admin and public routes...'
Test-HttpUrl -Uri "$adminBase/login" -SuccessMessage 'Admin login route is reachable on port 5174.' -FailureMessage 'Admin login route is not reachable on port 5174.'
Test-HttpUrl -Uri "$traceBase/t/$publicTraceToken" -SuccessMessage "Public trace route $publicTraceToken is reachable on port 5173." -FailureMessage "Public trace route $publicTraceToken is not reachable on port 5173."
Test-HttpUrl -Uri "$traceBase/t/$riskTraceToken" -SuccessMessage "Risk trace route $riskTraceToken is reachable on port 5173." -FailureMessage "Risk trace route $riskTraceToken is not reachable on port 5173."

Write-Step 'Checking fixed demo accounts...'
foreach ($account in $accountChecks) {
    $token = Test-AccountLogin -Account $account
    if ($account.Username -eq 'platform' -and $token) {
        $platformToken = $token
    }
}

Write-Step 'Checking fixed public trace tokens...'
try {
    $trace = Get-JsonResponse -Uri "$backendBase/api/public/traces/$publicTraceToken"
    if ($trace.success -and $trace.data.summary.batchCode -eq $starBatchCode) {
        Write-Pass "Public trace token $publicTraceToken points to $starBatchCode."
    }
    else {
        Write-Fail "Public trace token $publicTraceToken did not resolve to $starBatchCode."
    }
}
catch {
    Write-Fail "Public trace token $publicTraceToken is not reachable from backend."
}

try {
    $riskTrace = Get-JsonResponse -Uri "$backendBase/api/public/traces/$riskTraceToken"
    if ($riskTrace.success -and $riskTrace.data.summary.batchCode -eq $recallBatchCode) {
        Write-Pass "Risk trace token $riskTraceToken points to $recallBatchCode."
    }
    else {
        Write-Fail "Risk trace token $riskTraceToken did not resolve to $recallBatchCode."
    }
}
catch {
    Write-Fail "Risk trace token $riskTraceToken is not reachable from backend."
}

Write-Step 'Checking fixed batch baseline...'
if ([string]::IsNullOrWhiteSpace($platformToken)) {
    Write-Fail 'Fixed batch baseline check was skipped because platform login did not return a token.'
}
else {
    try {
        $batches = Get-JsonResponse -Uri "$backendBase/api/batches" -Headers @{ Authorization = "Bearer $platformToken" }
        $items = @($batches.data)

        $starBatch = $items | Where-Object { $_.batchCode -eq $starBatchCode } | Select-Object -First 1
        $pendingBatch = $items | Where-Object { $_.batchCode -eq $pendingBatchCode } | Select-Object -First 1
        $riskBatch = $items | Where-Object { $_.batchCode -eq $riskBatchCode } | Select-Object -First 1
        $recallBatch = $items | Where-Object { $_.batchCode -eq $recallBatchCode } | Select-Object -First 1

        if ($starBatch -and $starBatch.status -eq 'PUBLISHED' -and $starBatch.qrToken -eq $publicTraceToken) {
            Write-Pass "Star batch $starBatchCode is published and bound to $publicTraceToken."
        }
        else {
            Write-Fail "Star batch $starBatchCode is missing or not in the expected published state."
        }

        if ($pendingBatch -and $pendingBatch.status -eq 'DRAFT' -and $pendingBatch.qualityStatusCode -eq 'PENDING') {
            Write-Pass "Pending batch $pendingBatchCode is available for quality and field-work demo."
        }
        else {
            Write-Fail "Pending batch $pendingBatchCode is missing or not in the expected pending-quality state."
        }

        if ($riskBatch -and $riskBatch.status -eq 'FROZEN') {
            Write-Pass "Risk batch $riskBatchCode is fixed in the frozen state."
        }
        else {
            Write-Fail "Risk batch $riskBatchCode is missing or not frozen."
        }

        if ($recallBatch -and $recallBatch.status -eq 'RECALLED') {
            Write-Pass "Recall batch $recallBatchCode is fixed in the recalled state."
        }
        else {
            Write-Fail "Recall batch $recallBatchCode is missing or not recalled."
        }
    }
    catch {
        Write-Fail 'Authenticated batch baseline check failed.'
    }
}

Write-Step 'Checking star batch detail payload...'
if ([string]::IsNullOrWhiteSpace($platformToken)) {
    Write-Fail 'Star batch detail check was skipped because platform login did not return a token.'
}
else {
    try {
        $detail = Get-JsonResponse -Uri "$backendBase/api/batches/2" -Headers @{ Authorization = "Bearer $platformToken" }
        if ($detail.success -and
            $detail.data.batch.batchCode -eq $starBatchCode -and
            $detail.data.qr.generated -and
            $detail.data.quality.latestReport.reportNo -and
            $detail.data.trace.recentRecords.Count -ge 1) {
            Write-Pass "Star batch detail payload for $starBatchCode is complete enough for the final demo."
        }
        else {
            Write-Fail "Star batch detail payload for $starBatchCode is incomplete."
        }
    }
    catch {
        Write-Fail 'Star batch detail check failed.'
    }
}

Write-Step 'Quick links'
Write-Host ("  Admin login      : {0}/login" -f $adminBase)
Write-Host ("  Dashboard        : {0}/dashboard" -f $adminBase)
Write-Host ("  Batch list       : {0}/batches" -f $adminBase)
Write-Host ("  Star batch       : {0}/batches/2" -f $adminBase)
Write-Host ("  Public trace     : {0}/t/{1}" -f $traceBase, $publicTraceToken)
Write-Host ("  Risk trace       : {0}/t/{1}" -f $traceBase, $riskTraceToken)

if ($failures -gt 0) {
    exit 1
}
