param(
    [int[]] $BackendPorts = @(8081, 8082, 8083),
    [int]   $GatewayPort = 8080
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = $root
$gatewayDir = Join-Path $root "gateway-service"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Banking System Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Check if Maven is available
try {
    $mvnVersion = mvn --version 2>&1 | Out-String
    Write-Host "Maven found: $($mvnVersion.Split("`n")[0])" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Maven not found! Please install Maven first." -ForegroundColor Red
    exit 1
}

Write-Host "`nStarting backend instances on ports: $($BackendPorts -join ', ')" -ForegroundColor Cyan

$jobs = @()
foreach ($p in $BackendPorts) {
    $job = Start-Job -ScriptBlock {
        param($dir, $port)
        $ErrorActionPreference = "Continue"
        Set-Location $dir
        $env:PORT = $port
        Write-Host "Starting backend on port $port..." -ForegroundColor Cyan
        mvn spring-boot:run 2>&1
    } -ArgumentList $backendDir, $p
    $jobs += $job
    Write-Host "  Backend on port $p started (Job ID: $($job.Id))" -ForegroundColor Yellow
}

Write-Host "`nStarting gateway on port $GatewayPort" -ForegroundColor Cyan
# Use the dedicated gateway startup script
$gatewayScript = Join-Path $root "start-gateway.ps1"
if (Test-Path $gatewayScript) {
    $gatewayJob = & $gatewayScript -GatewayPort $GatewayPort
    if ($gatewayJob) {
        $jobs += $gatewayJob
        Write-Host "  Gateway on port $GatewayPort started (Job ID: $($gatewayJob.Id))" -ForegroundColor Yellow
    } else {
        Write-Host "  Warning: Gateway job was not returned" -ForegroundColor Yellow
    }
} else {
    Write-Host "  Warning: start-gateway.ps1 not found, using fallback method" -ForegroundColor Yellow
    $gatewayJob = Start-Job -ScriptBlock {
        param($dir, $port)
        $ErrorActionPreference = "Continue"
        Set-Location $dir
        $env:SERVER_PORT = $port
        Write-Host "Starting gateway on port $port..." -ForegroundColor Cyan
        mvn spring-boot:run 2>&1
    } -ArgumentList $gatewayDir, $GatewayPort
    $jobs += $gatewayJob
    Write-Host "  Gateway on port $GatewayPort started (Job ID: $($gatewayJob.Id))" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "All jobs started successfully!" -ForegroundColor Green
Write-Host "Job IDs: $($jobs.Id -join ', ')" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`nWaiting for services to start (this may take 30-60 seconds)..." -ForegroundColor Yellow
Write-Host "You can check service status with: .\check-services.ps1" -ForegroundColor Cyan
Write-Host "You can view job output with: Receive-Job -Id <job-id>" -ForegroundColor Cyan
Write-Host "You can stop all services with: .\stop-services.ps1" -ForegroundColor Cyan

# Wait a bit and check initial status
Write-Host "`nWaiting 10 seconds for services to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "`nInitial status check:" -ForegroundColor Cyan
foreach ($port in ($BackendPorts + @($GatewayPort))) {
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        Write-Host "  Port $port : Running" -ForegroundColor Green
    } else {
        Write-Host "  Port $port : Starting..." -ForegroundColor Yellow
    }
}

# Check for immediate errors
Write-Host "`nChecking for startup errors..." -ForegroundColor Cyan
foreach ($job in $jobs) {
    $output = Receive-Job -Id $job.Id -ErrorAction SilentlyContinue
    if ($output) {
        $errors = $output | Where-Object { $_ -match "ERROR|Exception|Failed to start|Cannot start" } | Select-Object -First 5
        if ($errors) {
            Write-Host "  Job $($job.Id) has errors:" -ForegroundColor Red
            $errors | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        }
    }
}

Write-Host "`nTip: Run .\show-job-logs.ps1 to see detailed logs" -ForegroundColor Cyan
Write-Host "Tip: Run .\watch-services.ps1 to monitor startup progress" -ForegroundColor Cyan
