#Requires -RunAsAdministrator

Write-Host "🚀 Complete Docker Virtualization Fix" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if running as administrator
function Test-Administrator {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Administrator)) {
    Write-Host "❌ This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Please right-click and 'Run as Administrator'" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit
}

Write-Host "✅ Running with Administrator privileges" -ForegroundColor Green
Write-Host ""

# Check virtualization status
Write-Host "🔍 Checking virtualization status..." -ForegroundColor Yellow
$wmicResult = wmic cpu get VirtualizationFirmwareEnabled /value 2>$null
if ($wmicResult -match "TRUE") {
    Write-Host "✅ CPU virtualization is enabled" -ForegroundColor Green
} else {
    Write-Host "❌ CPU virtualization is NOT enabled" -ForegroundColor Red
    Write-Host "Please enable VT-x/AMD-V in your BIOS settings" -ForegroundColor Red
    Write-Host "Press any key to continue anyway..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

Write-Host ""

# Check Hyper-V status and disable if enabled
Write-Host "🔧 Checking Hyper-V status..." -ForegroundColor Yellow
try {
    $hyperV = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -ErrorAction Stop
    if ($hyperV.State -eq "Enabled") {
        Write-Host "⚠️  Hyper-V is enabled and may conflict with Docker Desktop" -ForegroundColor Yellow
        $disableHyperV = Read-Host "Do you want to disable Hyper-V? (Y/N)"
        if ($disableHyperV -eq "Y" -or $disableHyperV -eq "y") {
            Write-Host "Disabling Hyper-V..." -ForegroundColor Yellow
            Disable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -NoRestart
            Write-Host "✅ Hyper-V disabled successfully" -ForegroundColor Green
            $restartRequired = $true
        } else {
            Write-Host "⚠️  Keeping Hyper-V enabled" -ForegroundColor Yellow
        }
    } else {
        Write-Host "✅ Hyper-V is already disabled" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Could not check Hyper-V status (this is normal on some systems)" -ForegroundColor Yellow
}

Write-Host ""

# Enable required Windows features
Write-Host "🔧 Enabling required Windows features..." -ForegroundColor Yellow
try {
    $vmpFeature = Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform
    if ($vmpFeature.State -ne "Enabled") {
        Write-Host "Enabling Virtual Machine Platform..." -ForegroundColor Yellow
        Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All -NoRestart
        Write-Host "✅ Virtual Machine Platform enabled" -ForegroundColor Green
        $restartRequired = $true
    } else {
        Write-Host "✅ Virtual Machine Platform already enabled" -ForegroundColor Green
    }

    $wslFeature = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux
    if ($wslFeature.State -ne "Enabled") {
        Write-Host "Enabling Windows Subsystem for Linux..." -ForegroundColor Yellow
        Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux -All -NoRestart
        Write-Host "✅ Windows Subsystem for Linux enabled" -ForegroundColor Green
        $restartRequired = $true
    } else {
        Write-Host "✅ Windows Subsystem for Linux already enabled" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Error enabling Windows features: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Set WSL2 as default
Write-Host "🔧 Setting WSL2 as default version..." -ForegroundColor Yellow
try {
    wsl --set-default-version 2 2>$null
    Write-Host "✅ WSL2 set as default" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not set WSL2 as default (may already be set)" -ForegroundColor Yellow
}

Write-Host ""

# Final instructions
Write-Host "🎉 Docker virtualization fix completed!" -ForegroundColor Green
Write-Host ""

if ($restartRequired) {
    Write-Host "⚠️  IMPORTANT: A system restart is required for changes to take effect." -ForegroundColor Red
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Close this window" -ForegroundColor White
    Write-Host "2. Restart your computer" -ForegroundColor White
    Write-Host "3. Start Docker Desktop after restart" -ForegroundColor White
    Write-Host "4. Run: docker-compose -f docker-compose.dev.yml up -d" -ForegroundColor White
    Write-Host "5. Access Grafana at http://localhost:3000 (admin/admin)" -ForegroundColor White
} else {
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Close this window" -ForegroundColor White
    Write-Host "2. Start Docker Desktop" -ForegroundColor White
    Write-Host "3. Run: docker-compose -f docker-compose.dev.yml up -d" -ForegroundColor White
    Write-Host "4. Access Grafana at http://localhost:3000 (admin/admin)" -ForegroundColor White
}

Write-Host ""
Write-Host "Your Spring Boot application is already running at http://localhost:8080" -ForegroundColor Green
Write-Host "All actuator endpoints are available for monitoring!" -ForegroundColor Green

Write-Host ""
Read-Host "Press Enter to close this window"

