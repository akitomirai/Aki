$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $root 'tests\e2e\scripts\final-demo-baseline-check.mjs'
$testDir = Join-Path $root 'tests\e2e'

if (-not (Test-Path $scriptPath)) {
    throw "Final demo verification script was not found: $scriptPath"
}

$env:ADMIN_BASE_URL = 'http://127.0.0.1:5174'
$env:TRACE_BASE_URL = 'http://127.0.0.1:5173'
$env:API_BASE_URL = 'http://127.0.0.1:8080/api'

Push-Location $testDir
try {
    node $scriptPath
    if ($LASTEXITCODE -ne 0) {
        throw "Final demo verification failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
