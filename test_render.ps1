$baseUrl = "https://pubgconnect-backend.onrender.com"
Write-Host "Testing Live Render Backend: $baseUrl" -ForegroundColor Cyan

$body = @{
    email = "ali@pubg.com"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 35
    Write-Host "`n=== LIVE CLOUD TEST SUCCEEDED! ===" -ForegroundColor Green
    Write-Host "Token: $($response.token)" -ForegroundColor Yellow
    Write-Host "User: $($response.user.username) | Friend ID: $($response.user.friendId)" -ForegroundColor Green
    Write-Host "Status: $($response.user.status)" -ForegroundColor White
} catch {
    Write-Host "Render error: $($_.Exception.Message)" -ForegroundColor Red
}
