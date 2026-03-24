# 演示环境自检脚本 (Precheck)

# 1. 检查后端服务 (端口 8080)
Write-Host "Checking Backend Service (Port 8080)..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body (@{username="platform"; password="123456"} | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    if ($response.code -eq 0) {
        Write-Host "[PASS] Backend Service is running and login is OK." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Backend Service returned error: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Backend Service is NOT reachable. Please check if Spring Boot is running." -ForegroundColor Red
}

# 2. 检查 Admin-Web (端口 5174)
Write-Host "`nChecking Admin-Web (Port 5174)..." -ForegroundColor Cyan
try {
    $tcpConnection = Test-NetConnection -ComputerName localhost -Port 5174 -InformationLevel Quiet
    if ($tcpConnection) {
        Write-Host "[PASS] Admin-Web is running on Port 5174." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Admin-Web Port 5174 is NOT open. Please run 'npm run dev' in admin-web." -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Admin-Web Port 5174 check error." -ForegroundColor Red
}

# 3. 检查 Trace-Web (端口 5173)
Write-Host "`nChecking Trace-Web (Port 5173)..." -ForegroundColor Cyan
try {
    $tcpConnection = Test-NetConnection -ComputerName localhost -Port 5173 -InformationLevel Quiet
    if ($tcpConnection) {
        Write-Host "[PASS] Trace-Web is running on Port 5173." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Trace-Web Port 5173 is NOT open. Please run 'npm run dev' in trace-web." -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Trace-Web Port 5173 check error." -ForegroundColor Red
}

# 4. 检查演示数据 (批次 1)
Write-Host "`nChecking Demo Data (Batch ID: 1)..." -ForegroundColor Cyan
try {
    $batchDetail = Invoke-RestMethod -Uri "http://localhost:8080/api/public/trace/detail/test-token-2026" -Method Get -ErrorAction Stop
    if ($batchDetail.code -eq 0 -and $batchDetail.data.batchId -eq 1) {
        Write-Host "[PASS] Demo Data (Batch 1) and Token (test-token-2026) are available." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Demo Data check failed: $($batchDetail.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Demo Data API check error. Backend might be down or token is invalid." -ForegroundColor Red
}

Write-Host "`nPrecheck Completed!" -ForegroundColor Yellow
