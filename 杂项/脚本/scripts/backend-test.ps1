[CmdletBinding()]
param(
    [ValidateSet('package', 'test', 'all')]
    [string]$Task = 'all'
)

$ErrorActionPreference = 'Stop'

$envInfo = . (Join-Path $PSScriptRoot 'backend-env.ps1') -Quiet
$backendDir = $envInfo.BackendDir

function Invoke-MavenCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    Write-Host ''
    Write-Host ('mvn ' + ($Arguments -join ' '))
    & mvn @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven command failed: mvn $($Arguments -join ' ')"
    }
}

Push-Location $backendDir
try {
    switch ($Task) {
        'package' {
            Invoke-MavenCommand -Arguments @('clean', 'package', '-DskipTests')
        }
        'test' {
            Invoke-MavenCommand -Arguments @('test')
        }
        'all' {
            Invoke-MavenCommand -Arguments @('clean', 'package', '-DskipTests')
            Invoke-MavenCommand -Arguments @('test')
        }
    }
}
finally {
    Pop-Location
}
