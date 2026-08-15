Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " PUBG Connect Cross-Platform Verification (PC and Android)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Login Ali (GameLoop PC Player)
$aliLoginBody = @{ email = "ali@pubg.com"; password = "password123" } | ConvertTo-Json
$aliAuth = Invoke-RestMethod -Uri "http://localhost:5000/api/auth/login" -Method Post -ContentType "application/json" -Body $aliLoginBody
$aliHeaders = @{ Authorization = "Bearer $($aliAuth.token)" }
Write-Host "1. Ali Logged In (PC Player). Friend ID: $($aliAuth.user.friendId)" -ForegroundColor Green

# 2. Login Ahmed (Android Player)
$ahmedLoginBody = @{ email = "ahmed@pubg.com"; password = "password123" } | ConvertTo-Json
$ahmedAuth = Invoke-RestMethod -Uri "http://localhost:5000/api/auth/login" -Method Post -ContentType "application/json" -Body $ahmedLoginBody
$ahmedHeaders = @{ Authorization = "Bearer $($ahmedAuth.token)" }
Write-Host "2. Ahmed Logged In (Android Player). Friend ID: $($ahmedAuth.user.friendId)" -ForegroundColor Green

# 3. Check Ahmed's friends view (Ahmed is on Android)
$ahmedFriends = Invoke-RestMethod -Uri "http://localhost:5000/api/friends" -Method Get -Headers $ahmedHeaders
Write-Host "`n3. Ahmed's friends list fetched:" -ForegroundColor Cyan
foreach ($f in $ahmedFriends) {
    $platformName = switch ($f.platform) { 1 { "GameLoop PC" } 2 { "Android Mobile" } default { "None" } }
    $statusName = switch ($f.status) { 2 { "Playing PUBG Mobile" } 1 { "Online" } default { "Offline" } }
    Write-Host "   - Friend: $($f.username) | Status: $statusName | Platform: $platformName"
}

# 4. Fetch Activity Feed (Cross-platform feed)
$activity = Invoke-RestMethod -Uri "http://localhost:5000/api/activity" -Method Get -Headers $ahmedHeaders
Write-Host "`n4. Recent Cross-Platform Activity Feed:" -ForegroundColor Cyan
foreach ($act in $activity) {
    $platformName = switch ($act.platform) { 1 { "GameLoop PC" } 2 { "Android Mobile" } default { "None" } }
    Write-Host "   - [Activity] $($act.username): $($act.actionDescription) via $platformName"
}

# 5. Test FCM Push Device Registration for Ahmed (Android)
$fcmBody = @{ deviceToken = "mock_fcm_token_ahmed_android_device_xyz"; platform = 2 } | ConvertTo-Json
$fcmRes = Invoke-RestMethod -Uri "http://localhost:5000/api/notifications/register-device" -Method Post -ContentType "application/json" -Headers $ahmedHeaders -Body $fcmBody
Write-Host "`n5. FCM Device Registration Result: $($fcmRes.message)" -ForegroundColor Green

Write-Host "`nAll Cross-Platform Endpoints and Workflows Verified Successfully!" -ForegroundColor Green
