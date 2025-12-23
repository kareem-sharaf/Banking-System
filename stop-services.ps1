param(
    [int[]] $BackendPorts = @(8081, 8082, 8083),
    [int]   $GatewayPort = 8080
)

Write-Host "Stopping all services..." -ForegroundColor Yellow

# Stop all PowerShell jobs
$allJobs = Get-Job
if ($allJobs) {
    Write-Host "Stopping PowerShell jobs..." -ForegroundColor Cyan
    $allJobs | Stop-Job
    $allJobs | Remove-Job -Force
    Write-Host "All jobs stopped and removed." -ForegroundColor Green
} else {
    Write-Host "No PowerShell jobs found." -ForegroundColor Gray
}

# Function to kill process on a specific port
function Stop-ProcessOnPort {
    param([int]$Port)
    
    try {
        $connection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connection) {
            $processId = $connection.OwningProcess | Select-Object -Unique
            foreach ($pid in $processId) {
                $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($process) {
                    Write-Host "Stopping process $($process.ProcessName) (PID: $pid) on port $Port" -ForegroundColor Cyan
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                }
            }
        } else {
            Write-Host "No process found on port $Port" -ForegroundColor Gray
        }
    } catch {
        Write-Host "Could not check port $Port : $_" -ForegroundColor Yellow
    }
}

# Stop processes on all ports
$allPorts = $BackendPorts + @($GatewayPort)
Write-Host "`nChecking and stopping processes on ports: $($allPorts -join ', ')" -ForegroundColor Cyan

foreach ($port in $allPorts) {
    Stop-ProcessOnPort -Port $port
}

Write-Host "`nAll services stopped!" -ForegroundColor Green

