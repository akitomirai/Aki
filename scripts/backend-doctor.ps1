[CmdletBinding()]
param(
    [string]$HealthUrl = 'http://127.0.0.1:8080/actuator/health'
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'backend-env.ps1') -Quiet -SkipVersionOutput | Out-Null

$expectedJavaHome = 'C:\Program Files\Java\jdk-21'
$failures = New-Object System.Collections.Generic.List[string]

function Assert-Check {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,
        [Parameter(Mandatory = $true)]
        [string]$SuccessMessage,
        [Parameter(Mandatory = $true)]
        [string]$FailureMessage
    )

    if ($Condition) {
        Write-Host "[OK]   $SuccessMessage" -ForegroundColor Green
        return
    }

    Write-Host "[FAIL] $FailureMessage" -ForegroundColor Red
    $failures.Add($FailureMessage)
}

$javaVersionOutput = (& cmd.exe /c 'java -version 2>&1' | Out-String).Trim()
$mavenVersionOutput = (& cmd.exe /c 'mvn -version 2>&1' | Out-String).Trim()

Assert-Check `
    -Condition ($env:JAVA_HOME -eq $expectedJavaHome) `
    -SuccessMessage "JAVA_HOME = $env:JAVA_HOME" `
    -FailureMessage "JAVA_HOME is not pinned to $expectedJavaHome"

Assert-Check `
    -Condition ($javaVersionOutput -match 'version "21\.') `
    -SuccessMessage "java -version reports JDK 21" `
    -FailureMessage "java -version did not report JDK 21"

Assert-Check `
    -Condition ($mavenVersionOutput -match 'Apache Maven') `
    -SuccessMessage 'mvn -version is available' `
    -FailureMessage 'mvn -version did not execute successfully'

Assert-Check `
    -Condition ($mavenVersionOutput -match 'Java version:\s*21\.') `
    -SuccessMessage 'mvn -version is bound to Java 21' `
    -FailureMessage 'mvn -version is not using Java 21'

try {
    $health = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 5
    $healthJson = $health | ConvertTo-Json -Compress
    Assert-Check `
        -Condition ($health.status -eq 'UP') `
        -SuccessMessage "$HealthUrl => $healthJson" `
        -FailureMessage "$HealthUrl did not return status UP"
}
catch {
    Assert-Check `
        -Condition $false `
        -SuccessMessage '' `
        -FailureMessage "$HealthUrl is not reachable: $($_.Exception.Message)"
}

Write-Host ''
Write-Host 'java -version'
Write-Host $javaVersionOutput
Write-Host ''
Write-Host 'mvn -version'
Write-Host $mavenVersionOutput

if ($failures.Count -gt 0) {
    throw "backend-doctor found $($failures.Count) problem(s)."
}
