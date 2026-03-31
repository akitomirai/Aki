param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('backend', 'adminWeb', 'traceWeb')]
    [string]$Service,
    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot,
    [Parameter(Mandatory = $true)]
    [string]$LogPath,
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'

$logDir = Split-Path -Parent $LogPath
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

Add-Content -Path $LogPath -Value ("[{0}] starting {1}" -f (Get-Date).ToString('s'), $Service)

switch ($Service) {
    'backend' {
        if ([string]::IsNullOrWhiteSpace($JavaHome)) {
            throw 'JavaHome is required for backend.'
        }

        $mavenCmd = 'C:\Program Files\Java\apache-maven-3.8.8\bin\mvn.cmd'
        if (-not (Test-Path $mavenCmd)) {
            throw "Maven was not found: $mavenCmd"
        }

        $env:JAVA_HOME = $JavaHome
        $env:Path = "$JavaHome\bin;C:\Program Files\Java\apache-maven-3.8.8\bin;" + $env:Path
        Set-Location (Join-Path $ProjectRoot 'backend')
        & $mavenCmd '-q' '-DskipTests' 'spring-boot:run' '-Dspring-boot.run.profiles=demo' *>> $LogPath
        break
    }
    'adminWeb' {
        $nodeExe = (Get-Command node -ErrorAction Stop).Source
        $vitePath = Join-Path $ProjectRoot 'admin-web\node_modules\vite\bin\vite.js'
        if (-not (Test-Path $vitePath)) {
            throw "Vite was not found: $vitePath"
        }

        Set-Location (Join-Path $ProjectRoot 'admin-web')
        & $nodeExe $vitePath '--host' '127.0.0.1' '--port' '5174' '--strictPort' *>> $LogPath
        break
    }
    'traceWeb' {
        $nodeExe = (Get-Command node -ErrorAction Stop).Source
        $vitePath = Join-Path $ProjectRoot 'trace-web\node_modules\vite\bin\vite.js'
        if (-not (Test-Path $vitePath)) {
            throw "Vite was not found: $vitePath"
        }

        Set-Location (Join-Path $ProjectRoot 'trace-web')
        & $nodeExe $vitePath '--host' '127.0.0.1' '--port' '5173' '--strictPort' *>> $LogPath
        break
    }
}
