param(
    [int[]] $BackendPorts = @(8081, 8082, 8083),
    [int]   $GatewayPort = 8080
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = $root
$gatewayDir = Join-Path $root "gateway-service"

Write-Host "Starting backend instances on ports: $($BackendPorts -join ', ')" -ForegroundColor Cyan

$jobs = @()
foreach ($p in $BackendPorts) {
    $jobs += Start-Job -ScriptBlock {
        param($dir, $port)
        Set-Location $dir
        $env:PORT = $port
        mvn spring-boot:run
    } -ArgumentList $backendDir, $p
}

Write-Host "Starting gateway on port $GatewayPort" -ForegroundColor Cyan
$jobs += Start-Job -ScriptBlock {
    param($dir, $port)
    Set-Location $dir
    $env:SERVER_PORT = $port
    mvn spring-boot:run
} -ArgumentList $gatewayDir, $GatewayPort

Write-Host "Jobs started (IDs): $($jobs.Id -join ', ')" -ForegroundColor Green
Write-Host "Use Get-Job / Receive-Job to monitor, Stop-Job <id> to stop."
