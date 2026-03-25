param(
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\round8-demo-common.ps1"

$paths = Get-Round8Paths
$tracked = Read-Round8State

if (-not $Quiet) {
    Write-Host "Stopping round 8 demo services..." -ForegroundColor Cyan
    if ($tracked.Count -gt 0) {
        $tracked | ForEach-Object {
            Write-Host ("  tracked: {0} pid={1}" -f $_.name, $_.pid)
        }
    } else {
        Write-Host "  no tracked process file found, falling back to demo ports 8080/5173/5174."
    }
}

Stop-Round8Processes -Quiet:$Quiet

$remaining = Get-PortUsageSummary -Ports @(8080, 5173, 5174)
if ($remaining) {
    Write-Host "Some demo ports are still occupied after cleanup:" -ForegroundColor Yellow
    $remaining | ForEach-Object {
        Write-Host ("  port {0} -> pid {1}" -f $_.LocalPort, $_.OwningProcess) -ForegroundColor Yellow
    }
} elseif (-not $Quiet) {
    Write-Host "Round 8 demo services have been stopped." -ForegroundColor Green
}

if (Test-Path $paths.StateFile) {
    Clear-Round8State
}
