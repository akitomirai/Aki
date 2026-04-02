$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$jsonPath = Join-Path $root 'output\playwright\full-regression-baseline.json'
$scriptPath = Join-Path $root 'output\playwright\full-regression-baseline.mjs'
$screenshotDir = Join-Path $root 'output\playwright'
$playwrightDir = Join-Path $root 'tests\e2e'
$logPermissionSpec = 'specs/log-permission-smoke.spec.mjs'

function Wait-HttpOk {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 20
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                return $true
            }
        }
        catch {
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

function Ensure-Service {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    if (-not (Wait-HttpOk -Url $Url)) {
        throw "$Name is not available: $Url"
    }
}

function To-Array {
    param($Value)

    if ($null -eq $Value) {
        return @()
    }
    return @($Value)
}

function Add-Issue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    $script:issues += $Message
}

function Invoke-LogPermissionSmoke {
    $env:ADMIN_BASE_URL = 'http://127.0.0.1:5174'
    $env:TRACE_BASE_URL = 'http://127.0.0.1:5173'
    $env:API_BASE_URL = 'http://127.0.0.1:8080/api'

    Write-Host "Running log permission smoke: $logPermissionSpec" -ForegroundColor Cyan
    Push-Location $playwrightDir
    try {
        npm run test -- $logPermissionSpec
        if ($LASTEXITCODE -ne 0) {
            throw "Log permission smoke failed with exit code: $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host 'Checking runtime before baseline regression...' -ForegroundColor Cyan
Ensure-Service -Name 'backend' -Url 'http://127.0.0.1:8080/actuator/health'
Ensure-Service -Name 'admin-web' -Url 'http://127.0.0.1:5174/login'
Ensure-Service -Name 'trace-web' -Url 'http://127.0.0.1:5173'

if (-not (Test-Path $scriptPath)) {
    throw "Regression entry was not found: $scriptPath"
}

$node = (Get-Command node -ErrorAction Stop).Source
Write-Host "Running baseline regression: $scriptPath" -ForegroundColor Cyan
& $node $scriptPath

if ($LASTEXITCODE -ne 0) {
    throw "Regression entry failed with exit code: $LASTEXITCODE"
}

if (-not (Test-Path $jsonPath)) {
    throw "Regression JSON result was not found: $jsonPath"
}

$result = Get-Content $jsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
$issues = @()

Invoke-LogPermissionSmoke

if ($result.seedBaseline.workbench.batchStatusCode -ne 'PUBLISHED') {
    Add-Issue "Seed baseline batch 2 code is not PUBLISHED: $($result.seedBaseline.workbench.batchStatusCode)"
}
if ([string]::IsNullOrWhiteSpace($result.seedBaseline.workbench.qualityStatus)) {
    Add-Issue 'Seed baseline quality status is empty.'
}
if ($result.seedBaseline.workbench.qualityStatus -match 'PASS|FAIL|PENDING') {
    Add-Issue "Seed baseline quality status still exposes a raw code: $($result.seedBaseline.workbench.qualityStatus)"
}
if ([string]::IsNullOrWhiteSpace($result.seedBaseline.workbench.qrStatus)) {
    Add-Issue 'Seed baseline QR status is empty.'
}
if ($result.seedBaseline.workbench.qrStatus -match 'ACTIVE|SUSPENDED|RECALLED|NOT_GENERATED') {
    Add-Issue "Seed baseline QR status still exposes a raw code: $($result.seedBaseline.workbench.qrStatus)"
}
if ($result.seedBaseline.workbench.task.assigneeUserId -ne 5) {
    Add-Issue "Seed baseline default assignee is not operator_support: $($result.seedBaseline.workbench.task.assigneeUserId)"
}
if ($result.seedBaseline.workbench.publicUrl -notlike '*/t/orange-202603-d1') {
    Add-Issue "Seed baseline public url is unexpected: $($result.seedBaseline.workbench.publicUrl)"
}
if ($result.seedBaseline.public.publishedAtText -ne '2026-03-21 11:30') {
    Add-Issue "Seed baseline publishedAt is unexpected: $($result.seedBaseline.public.publishedAtText)"
}

foreach ($pageName in 'batches', 'workbench', 'fieldEntry', 'publicTrace') {
    $pageAudit = $result.pageAudit.$pageName
    if ((To-Array $pageAudit.enums).Count -gt 0) {
        Add-Issue "$pageName still exposes raw enum values: $((To-Array $pageAudit.enums) -join ', ')"
    }
    if ((To-Array $pageAudit.englishSeeds).Count -gt 0) {
        Add-Issue "$pageName still exposes English seed values: $((To-Array $pageAudit.englishSeeds) -join ', ')"
    }
}

if ($result.pageAudit.publicTrace.publishedConflict) {
    Add-Issue 'Public trace page still has a published status / publishedAt conflict.'
}
if (-not $result.pageAudit.taskConsistency.allMatch) {
    Add-Issue 'Task status is inconsistent between workbench, list, and field entry.'
}
if (-not $result.pageAudit.riskConsistency.consistent) {
    Add-Issue 'Risk status is inconsistent between workbench and public trace page.'
}
if (-not $result.operatorSubmission.imageOrderMatches) {
    Add-Issue 'Latest record image order does not match the operator upload order.'
}
if ($result.finalState.supportDraftCount -ne 0) {
    Add-Issue "operator_support still has draft residue after regression: $($result.finalState.supportDraftCount)"
}

$unexpectedHttpErrors = To-Array $result.uiErrors.httpErrors | Where-Object {
    -not ($_.status -eq 400 -and $_.url -like '*/api/batches/2/assignment')
}
if ($unexpectedHttpErrors.Count -gt 0) {
    Add-Issue "Unexpected HTTP errors were captured: $($unexpectedHttpErrors | ConvertTo-Json -Compress)"
}

$unexpectedConsoleErrors = To-Array $result.uiErrors.consoleErrors | Where-Object {
    -not ($_.text -match 'status of 400' -and ($_.pageUrl -like '*/batches/2'))
}
if ($unexpectedConsoleErrors.Count -gt 0) {
    Add-Issue "Unexpected browser console errors were captured: $($unexpectedConsoleErrors | ConvertTo-Json -Compress)"
}

$pageErrors = To-Array $result.uiErrors.pageErrors
if ($pageErrors.Count -gt 0) {
    Add-Issue "Page script errors were captured: $($pageErrors | ConvertTo-Json -Compress)"
}

if ($issues.Count -gt 0) {
    Write-Host ''
    Write-Host 'Baseline regression FAILED:' -ForegroundColor Red
    $issues | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    Write-Host ''
    Write-Host "JSON Result : $jsonPath"
    Write-Host "Screenshots : $screenshotDir"
    exit 1
}

Write-Host ''
Write-Host 'Baseline regression PASSED.' -ForegroundColor Green
Write-Host "JSON Result : $jsonPath"
Write-Host "Screenshots : $screenshotDir"
Write-Host 'Verified gates:' -ForegroundColor Cyan
Write-Host '  - demo seed is restored to the published baseline'
Write-Host '  - assignment chain, draft blocking, operator submit, risk chain, and public linkage all passed'
Write-Host '  - log permission smoke passed for platform, enterprise_admin, and operator'
Write-Host '  - high-frequency pages do not expose English seed values or raw enum values'
Write-Host '  - publishedAt, task status, and risk status remain consistent'
