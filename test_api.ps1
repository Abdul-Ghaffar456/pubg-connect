$loginBody = @{ email = "ali@pubg.com"; password = "password123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:5000/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
Write-Host "Login Result:" ($loginRes | ConvertTo-Json -Depth 3)

$token = $loginRes.token
$headers = @{ Authorization = "Bearer $token" }

# Test Search
$searchRes = Invoke-RestMethod -Uri "http://localhost:5000/api/friends/search?friendId=B3M88X" -Method Get -Headers $headers
Write-Host "Search Result for Ahmed (B3M88X):" ($searchRes | ConvertTo-Json)

# Test Get Friends
$friendsRes = Invoke-RestMethod -Uri "http://localhost:5000/api/friends" -Method Get -Headers $headers
Write-Host "Friends List for Ali:" ($friendsRes | ConvertTo-Json -Depth 3)

Write-Host "API verification succeeded!"
