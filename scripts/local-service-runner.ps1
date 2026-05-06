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

function Start-ViteService {
    param(
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,
        [Parameter(Mandatory = $true)]
        [string]$VitePath,
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $nodeExe = (Get-Command node -ErrorAction Stop).Source
    $stderrPath = "$LogPath.stderr"

    if (-not (Test-Path $VitePath)) {
        throw "Vite was not found: $VitePath"
    }

    Set-Location $WorkingDirectory

    foreach ($path in @($LogPath, $stderrPath)) {
        if (-not (Test-Path $path)) {
            New-Item -ItemType File -Force -Path $path | Out-Null
        }
    }

    $arguments = @(
        "`"$VitePath`"",
        '--host', '0.0.0.0',
        '--port', [string]$Port,
        '--strictPort'
    )

    $viteProcess = Start-Process `
        -FilePath $nodeExe `
        -ArgumentList ($arguments -join ' ') `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $LogPath `
        -RedirectStandardError $stderrPath `
        -PassThru `
        -WindowStyle Hidden

    try {
        Wait-Process -Id $viteProcess.Id
    }
    finally {
        if ((Test-Path $stderrPath) -and ((Get-Item $stderrPath).Length -gt 0)) {
            Add-Content -Path $LogPath -Value ''
            Add-Content -Path $LogPath -Value ('[{0}] stderr' -f (Get-Date).ToString('s'))
            Get-Content -Path $stderrPath -Encoding UTF8 | Add-Content -Path $LogPath
        }
    }
}

function Get-LanIpAddress {
    $candidates = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object {
            $_.IPAddress -notlike '127.*' -and
            $_.IPAddress -notlike '169.254.*' -and
            $_.AddressState -eq 'Preferred' -and
            $_.InterfaceAlias -notmatch 'VMware|VirtualBox|Loopback|WSL|Hyper-V|vEthernet'
        } |
        Sort-Object `
            @{ Expression = { if ($_.InterfaceAlias -match 'WLAN|Wi-Fi|无线') { 0 } else { 1 } } },
            InterfaceMetric

    $ip = $candidates | Select-Object -First 1 -ExpandProperty IPAddress
    if ([string]::IsNullOrWhiteSpace($ip)) {
        $ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
            Where-Object {
                $_.IPAddress -notlike '127.*' -and
                $_.IPAddress -notlike '169.254.*' -and
                $_.AddressState -eq 'Preferred'
            } |
            Select-Object -First 1 -ExpandProperty IPAddress
    }

    return $ip
}

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
        $lanIp = Get-LanIpAddress
        if (-not [string]::IsNullOrWhiteSpace($lanIp) -and [string]::IsNullOrWhiteSpace($env:PUBLIC_TRACE_BASE_URL)) {
            $env:PUBLIC_TRACE_BASE_URL = "http://${lanIp}:5173"
            Add-Content -Path $LogPath -Value ("[{0}] PUBLIC_TRACE_BASE_URL={1}" -f (Get-Date).ToString('s'), $env:PUBLIC_TRACE_BASE_URL)
        }
        Set-Location (Join-Path $ProjectRoot 'backend')
        & $mavenCmd '-q' '-DskipTests' 'spring-boot:run' '-Dspring-boot.run.profiles=demo' *>> $LogPath
        break
    }
    'adminWeb' {
        $lanIp = Get-LanIpAddress
        if (-not [string]::IsNullOrWhiteSpace($lanIp) -and [string]::IsNullOrWhiteSpace($env:VITE_TRACE_WEB_ORIGIN)) {
            $env:VITE_TRACE_WEB_ORIGIN = "http://${lanIp}:5173"
            Add-Content -Path $LogPath -Value ("[{0}] VITE_TRACE_WEB_ORIGIN={1}" -f (Get-Date).ToString('s'), $env:VITE_TRACE_WEB_ORIGIN)
        }
        $vitePath = Join-Path $ProjectRoot 'admin-web\node_modules\vite\bin\vite.js'
        Start-ViteService -WorkingDirectory (Join-Path $ProjectRoot 'admin-web') -VitePath $vitePath -Port 5174
        break
    }
    'traceWeb' {
        $vitePath = Join-Path $ProjectRoot 'trace-web\node_modules\vite\bin\vite.js'
        Start-ViteService -WorkingDirectory (Join-Path $ProjectRoot 'trace-web') -VitePath $vitePath -Port 5173
        break
    }
}
