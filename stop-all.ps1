$ErrorActionPreference = "Continue"

function Stop-PortProcess {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        $lines = netstat -ano | Select-String ":$Port\s"

        if (-not $lines) {
            Write-Host "Port $Port is not in use." -ForegroundColor DarkGray
            return
        }

        $procIds = @()

        foreach ($line in $lines) {
            $text = ($line.ToString() -replace "\s+", " ").Trim()
            $parts = $text.Split(" ")
            $procIdText = $parts[-1]

            if ($procIdText -match "^\d+$" -and $procIdText -ne "0") {
                $procIds += [int]$procIdText
            }
        }

        $procIds = $procIds | Sort-Object -Unique

        foreach ($procId in $procIds) {
            try {
                $proc = Get-Process -Id $procId -ErrorAction Stop
                Write-Host "Stopping process on port $Port : PID=$procId Name=$($proc.ProcessName)" -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction Stop
            }
            catch {
                Write-Host "Failed to stop PID=$procId, or it already exited." -ForegroundColor DarkYellow
            }
        }
    }
    catch {
        Write-Host "Failed while checking/stopping port $Port : $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "Stopping project-related processes..." -ForegroundColor Cyan
Stop-PortProcess -Port 6379
Stop-PortProcess -Port 8080
Stop-PortProcess -Port 5174
Stop-PortProcess -Port 5173
Write-Host "Done." -ForegroundColor Green
Read-Host "Press Enter to exit"