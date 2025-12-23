param(
    [int[]] $BackendPorts = @(8081, 8082, 8083),
    [int]   $GatewayPort = 8080
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Banking System Services Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check PowerShell Jobs
Write-Host "`nPowerShell Jobs Status:" -ForegroundColor Yellow
$allJobs = Get-Job
if ($allJobs) {
    foreach ($job in $allJobs) {
        $status = $job.State
        $color = switch ($status) {
            "Running" { "Green" }
            "Completed" { "Yellow" }
            "Failed" { "Red" }
            default { "Gray" }
        }
        Write-Host "  Job ID $($job.Id): $status" -ForegroundColor $color
        
        # Show recent errors if any
        if ($status -eq "Failed" -or $status -eq "Completed") {
            $output = Receive-Job -Id $job.Id -ErrorAction SilentlyContinue
            if ($output) {
                $errors = $output | Where-Object { $_ -match "ERROR|Exception|Failed" } | Select-Object -Last 3
                if ($errors) {
                    Write-Host "    Recent errors:" -ForegroundColor Red
                    foreach ($err in $errors) {
                        Write-Host "      - $err" -ForegroundColor Red
                    }
                }
            }
        }
    }
} else {
    Write-Host "  No jobs found" -ForegroundColor Gray
}

# Check Ports
Write-Host "`nPort Status:" -ForegroundColor Yellow
$allPorts = $BackendPorts + @($GatewayPort)
$allRunning = $true

foreach ($port in $allPorts) {
    try {
        $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($connection) {
            $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
            $processName = if ($process) { $process.ProcessName } else { "Unknown" }
            Write-Host "  Port $port : Running (PID: $($connection.OwningProcess), Process: $processName)" -ForegroundColor Green
        } else {
            Write-Host "  Port $port : Not listening" -ForegroundColor Red
            $allRunning = $false
        }
    } catch {
        Write-Host "  Port $port : Error checking - $_" -ForegroundColor Red
        $allRunning = $false
    }
}

# Test HTTP connections
Write-Host "`nHTTP Health Checks:" -ForegroundColor Yellow
foreach ($port in $allPorts) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  Port $port : Healthy (HTTP 200)" -ForegroundColor Green
        } else {
            Write-Host "  Port $port : Responding but unhealthy (HTTP $($response.StatusCode))" -ForegroundColor Yellow
        }
    } catch {
        # Try regular endpoint for gateway
        if ($port -eq $GatewayPort) {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$port" -TimeoutSec 2 -ErrorAction SilentlyContinue
                Write-Host "  Port $port : Responding" -ForegroundColor Green
            } catch {
                Write-Host "  Port $port : Not responding to HTTP requests" -ForegroundColor Red
            }
        } else {
            Write-Host "  Port $port : Not responding to HTTP requests" -ForegroundColor Red
        }
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
if ($allRunning) {
    Write-Host "All services appear to be running!" -ForegroundColor Green
} else {
    Write-Host "Some services are not running. Check job output for errors." -ForegroundColor Yellow
    Write-Host "Use: Receive-Job -Id <job-id> to see logs" -ForegroundColor Cyan
}
Write-Host "========================================" -ForegroundColor Cyan

