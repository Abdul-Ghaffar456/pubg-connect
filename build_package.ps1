Get-Process -Name "PubgConnect.Client" -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Milliseconds 500

$distDir = "dist\PUBGConnect_v1.0_Windows_x64"
if (Test-Path $distDir) {
    Remove-Item -Path $distDir -Recurse -Force
}
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

Copy-Item "publish\client\PubgConnect.Client.exe" -Destination "$distDir\PubgConnect.Client.exe" -Force
Copy-Item "publish\server\PubgConnect.Server.exe" -Destination "$distDir\PubgConnect.Server.exe" -Force
Copy-Item "publish\Install.ps1" -Destination "$distDir\Install.ps1" -Force
Copy-Item "publish\Install.bat" -Destination "$distDir\Install.bat" -Force
Copy-Item "publish\Uninstall.bat" -Destination "$distDir\Uninstall.bat" -Force
Copy-Item "publish\Run_Client_Direct.bat" -Destination "$distDir\Run_Client_Direct.bat" -Force
Copy-Item "publish\Run_Server.bat" -Destination "$distDir\Run_Server.bat" -Force
Copy-Item "publish\README.txt" -Destination "$distDir\README.txt" -Force
if (Test-Path "publish\server_config.json") { Copy-Item "publish\server_config.json" -Destination "$distDir\server_config.json" -Force }

$zipPath = "dist\PUBGConnect_v1.0_Windows_x64.zip"
if (Test-Path $zipPath) {
    Remove-Item -Path $zipPath -Force
}

Write-Host "Creating ZIP Archive at $zipPath..." -ForegroundColor Cyan
Compress-Archive -Path "$distDir\*" -DestinationPath $zipPath -Force

$fileInfo = Get-Item $zipPath
$mbSize = [Math]::Round($fileInfo.Length / 1MB, 2)
Write-Host "Package created successfully: $zipPath ($mbSize MB)" -ForegroundColor Green
