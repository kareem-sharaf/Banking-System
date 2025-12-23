# Docker Virtualization Fix Script
# Run this script as Administrator

Write-Host "Docker Virtualization Fix Script" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green

# Check virtualization status
Write-Host "Checking virtualization status..." -ForegroundColor Yellow
$wmicResult = wmic cpu get VirtualizationFirmwareEnabled /value
if ($wmicResult -match "TRUE") {
    Write-Host "✓ CPU virtualization is enabled" -ForegroundColor Green
} else {
    Write-Host "✗ CPU virtualization is NOT enabled" -ForegroundColor Red
    Write-Host "Please enable VT-x/AMD-V in your BIOS settings" -ForegroundColor Red
}

# Disable Hyper-V if it's enabled (can conflict with Docker Desktop)
Write-Host "Checking Hyper-V status..." -ForegroundColor Yellow
$hyperV = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All
if ($hyperV.State -eq "Enabled") {
    Write-Host "Hyper-V is enabled. Disabling it..." -ForegroundColor Yellow
    Disable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -NoRestart
    Write-Host "✓ Hyper-V disabled. Please restart your computer." -ForegroundColor Green
} else {
    Write-Host "✓ Hyper-V is already disabled" -ForegroundColor Green
}

# Enable Virtual Machine Platform and Windows Subsystem for Linux
Write-Host "Enabling required Windows features..." -ForegroundColor Yellow
Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All -NoRestart
Write-Host "✓ Windows features enabled" -ForegroundColor Green

# Check if WSL2 is set as default
Write-Host "Setting WSL2 as default..." -ForegroundColor Yellow
wsl --set-default-version 2
Write-Host "✓ WSL2 set as default" -ForegroundColor Green

# Try to start Docker service
Write-Host "Starting Docker service..." -ForegroundColor Yellow
try {
    Start-Service com.docker.service
    Write-Host "✓ Docker service started" -ForegroundColor Green
} catch {
    Write-Host "⚠ Could not start Docker service automatically" -ForegroundColor Yellow
    Write-Host "Please start Docker Desktop manually after restart" -ForegroundColor Yellow
}

Write-Host "`nScript completed!" -ForegroundColor Green
Write-Host "Please restart your computer and then start Docker Desktop." -ForegroundColor Green

