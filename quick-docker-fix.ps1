# Quick Docker Fix - Run as Administrator
Write-Host "Quick Docker Fix" -ForegroundColor Green

# Check and disable Hyper-V
$hyperV = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All
if ($hyperV.State -eq "Enabled") {
    Write-Host "Disabling Hyper-V..." -ForegroundColor Yellow
    Disable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -NoRestart
    Write-Host "✓ Hyper-V disabled. Restart required." -ForegroundColor Green
}

# Enable required features
Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All -NoRestart

Write-Host "Please restart your computer, then start Docker Desktop." -ForegroundColor Green

