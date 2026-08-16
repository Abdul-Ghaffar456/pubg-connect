Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Building Single Setup Executable (.exe)" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

$scriptDir = $PSScriptRoot
if ([string]::IsNullOrEmpty($scriptDir)) { $scriptDir = (Get-Location).Path }

# 1. Publish Client
Write-Host "[1/4] Publishing PUBG Connect Client..." -ForegroundColor Yellow
dotnet publish src/PubgConnect.Client/PubgConnect.Client.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o publish/client

# Ensure app_icon.ico and server_config.json are in publish/client
Copy-Item "src/PubgConnect.Client/app_icon.ico" -Destination "publish/client/app_icon.ico" -Force
if (Test-Path "src/PubgConnect.Client/server_config.json") {
    Copy-Item "src/PubgConnect.Client/server_config.json" -Destination "publish/client/server_config.json" -Force
}

# 2. Package Client into Installer payload.zip
Write-Host "[2/4] Creating payload.zip for installer..." -ForegroundColor Yellow
$payloadZip = Join-Path $scriptDir "src\PubgConnect.Installer\payload.zip"
if (Test-Path $payloadZip) { Remove-Item $payloadZip -Force }

$clientDir = Join-Path $scriptDir "publish\client"
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($clientDir, $payloadZip)

# 3. Publish Single Setup Executable
Write-Host "[3/4] Compiling Single-File Setup Executable..." -ForegroundColor Yellow
if (-not (Test-Path "dist")) { New-Item -ItemType Directory -Path "dist" | Out-Null }

dotnet publish src/PubgConnect.Installer/PubgConnect.Installer.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o publish/setup

# 4. Copy to dist
Write-Host "[4/4] Outputting final setup file..." -ForegroundColor Yellow
Copy-Item "publish/setup/PubgConnect.Installer.exe" -Destination "dist/PUBGConnect_Setup_v1.0.exe" -Force

$setupFile = Get-Item "dist/PUBGConnect_Setup_v1.0.exe"
$sizeMb = [math]::Round($setupFile.Length / 1MB, 2)
Write-Host "=========================================" -ForegroundColor Green
Write-Host " Single Setup File Created Successfully!" -ForegroundColor Green
Write-Host " Location: dist\PUBGConnect_Setup_v1.0.exe ($sizeMb MB)" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
