param(
    [int] $GatewayPort = 8080
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$gatewayDir = Join-Path $root "gateway-service"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Gateway Service on Port $GatewayPort" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
 
# Check if Maven is available
try {
    $mvnVersion = mvn --version 2>&1 | Out-String
    Write-Host "Maven found: $($mvnVersion.Split("`n")[0])" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Maven not found! Please install Maven first." -ForegroundColor Red
    exit 1
}

# Check if port is already in use
$connection = Get-NetTCPConnection -LocalPort $GatewayPort -ErrorAction SilentlyContinue
if ($connection) {
    $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
    Write-Host "WARNING: Port $GatewayPort is already in use by process: $($process.ProcessName) (PID: $($connection.OwningProcess))" -ForegroundColor Yellow
    Write-Host "Stopping existing process..." -ForegroundColor Cyan
    Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "Process stopped. Starting gateway..." -ForegroundColor Green
}

# Check if gateway directory exists
if (-not (Test-Path $gatewayDir)) {
    Write-Host "ERROR: Gateway directory not found: $gatewayDir" -ForegroundColor Red
    exit 1
}

Write-Host "`nStarting gateway service..." -ForegroundColor Cyan
Write-Host "Gateway directory: $gatewayDir" -ForegroundColor Gray
Write-Host "Port: $GatewayPort" -ForegroundColor Gray

# Start gateway as a background job
$gatewayJob = Start-Job -ScriptBlock {
    param($dir, $port)
    $ErrorActionPreference = "Continue"
    # Change to gateway-service directory
    Set-Location $dir
    $env:SERVER_PORT = $port
    Write-Host "Current directory: $(Get-Location)" -ForegroundColor Cyan
    Write-Host "Starting gateway on port $port..." -ForegroundColor Cyan
    # Run mvn spring-boot:run from gateway-service directory
    & mvn spring-boot:run 2>&1
} -ArgumentList $gatewayDir, $GatewayPort

Write-Host "Gateway service started (Job ID: $($gatewayJob.Id))" -ForegroundColor Green
Write-Host "`nWaiting for gateway to start (this may take 30-60 seconds)..." -ForegroundColor Yellow

# Wait and check if port becomes available
$maxWait = 120 # seconds
$elapsed = 0
$checkInterval = 3 # seconds
$started = $false

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds $checkInterval
    $elapsed += $checkInterval
    
    $connection = Get-NetTCPConnection -LocalPort $GatewayPort -ErrorAction SilentlyContinue
    if ($connection) {
        Write-Host "`n[OK] Gateway is now running on port $GatewayPort!" -ForegroundColor Green
        $started = $true
        break
    }
    
    # Check for errors in job output
    $output = Receive-Job -Id $gatewayJob.Id -ErrorAction SilentlyContinue
    if ($output) {
        $errors = $output | Where-Object { $_ -match "ERROR|Exception|Failed to start|Cannot start" } | Select-Object -Last 3
        if ($errors) {
            Write-Host "`n[ERROR] Gateway startup errors detected:" -ForegroundColor Red
            $errors | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            break
        }
    }
    
    Write-Host "." -NoNewline -ForegroundColor Gray
}

if (-not $started) {
    Write-Host "`n[WARNING] Gateway did not start within $maxWait seconds." -ForegroundColor Yellow
    Write-Host "Check job output with: Receive-Job -Id $($gatewayJob.Id)" -ForegroundColor Cyan
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Job ID: $($gatewayJob.Id)" -ForegroundColor Cyan
Write-Host "View logs: Receive-Job -Id $($gatewayJob.Id)" -ForegroundColor Gray
Write-Host "Stop gateway: Stop-Job -Id $($gatewayJob.Id); Remove-Job -Id $($gatewayJob.Id)" -ForegroundColor Gray
Write-Host "Test gateway: Invoke-RestMethod -Uri http://localhost:$GatewayPort/actuator/health" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan

return $gatewayJob

