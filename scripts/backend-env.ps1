[CmdletBinding()]
param(
    [switch]$Quiet,
    [switch]$SkipVersionOutput
)

$ErrorActionPreference = 'Stop'

$requiredJavaHome = 'C:\Program Files\Java\jdk-21'
$preferredMavenHome = 'C:\Program Files\Java\apache-maven-3.8.8'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendDir = Join-Path $projectRoot 'backend'

function Prepend-ProcessPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PathEntry
    )

    $normalizedTarget = $PathEntry.Trim().ToLowerInvariant()
    $existing = @()
    if ($env:Path) {
        $existing = $env:Path.Split(';') | Where-Object { $_ -and $_.Trim() }
    }

    $filtered = foreach ($item in $existing) {
        if ($item.Trim().ToLowerInvariant() -ne $normalizedTarget) {
            $item
        }
    }

    $env:Path = (($PathEntry) + ';' + ($filtered -join ';')).TrimEnd(';')
}

if (-not (Test-Path (Join-Path $requiredJavaHome 'bin\java.exe'))) {
    throw "Required JDK was not found: $requiredJavaHome"
}

$env:JAVA_HOME = $requiredJavaHome
Prepend-ProcessPath -PathEntry (Join-Path $env:JAVA_HOME 'bin')

if (Test-Path (Join-Path $preferredMavenHome 'bin\mvn.cmd')) {
    $env:MAVEN_HOME = $preferredMavenHome
    Prepend-ProcessPath -PathEntry (Join-Path $env:MAVEN_HOME 'bin')
}

$mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
if ($null -eq $mvnCommand) {
    throw 'Maven was not found in the current session PATH.'
}

if (-not $Quiet) {
    Write-Host "Project Root : $projectRoot"
    Write-Host "Backend Dir  : $backendDir"
    Write-Host "JAVA_HOME    : $env:JAVA_HOME"
    if ($env:MAVEN_HOME) {
        Write-Host "MAVEN_HOME   : $env:MAVEN_HOME"
    }
}

if (-not $SkipVersionOutput) {
    & java -version
    if ($LASTEXITCODE -ne 0) {
        throw 'java -version failed.'
    }

    & mvn -version
    if ($LASTEXITCODE -ne 0) {
        throw 'mvn -version failed.'
    }
}

[pscustomobject]@{
    ProjectRoot = $projectRoot
    BackendDir = $backendDir
    JavaHome = $env:JAVA_HOME
    MavenHome = $env:MAVEN_HOME
}
