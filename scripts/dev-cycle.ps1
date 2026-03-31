param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('start', 'stop', 'check', 'reset', 'regression')]
    [string]$Mode
)

$ErrorActionPreference = 'Stop'

$scriptMap = @{
    start = 'start-local.ps1'
    stop = 'stop-local.ps1'
    check = 'check-local.ps1'
    reset = 'reset-demo-baseline.ps1'
    regression = 'run-baseline-regression.ps1'
}

$target = Join-Path $PSScriptRoot $scriptMap[$Mode]
if (-not (Test-Path $target)) {
    throw "Dev cycle target was not found: $target"
}

& 'powershell.exe' '-ExecutionPolicy' 'Bypass' '-File' $target
exit $LASTEXITCODE
