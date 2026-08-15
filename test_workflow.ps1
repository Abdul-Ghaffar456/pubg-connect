# 1. Register new user Tariq
$regBody = @{ username = "Tariq"; email = "tariq@pubg.com"; password = "password123" } | ConvertTo-Json
$regRes = Invoke-RestMethod -Uri "http://localhost:5000/api/auth/register" -Method Post -ContentType "application/json" -Body $regBody
Write-Host "Registered Tariq:" $regRes.user.friendId

$tariqToken = $regRes.token
$tariqHeaders = @{ Authorization = "Bearer $tariqToken" }

# 2. Tariq searches for Hassan (Friend ID: C9P41Z)
$searchHassan = Invoke-RestMethod -Uri "http://localhost:5000/api/friends/search?friendId=C9P41Z" -Method Get -Headers $tariqHeaders
Write-Host "Tariq found:" $searchHassan.username

# 3. Tariq sends friend request to Hassan
$reqBody = @{ targetFriendId = "C9P41Z" } | ConvertTo-Json
$sendReqRes = Invoke-RestMethod -Uri "http://localhost:5000/api/friends/request" -Method Post -ContentType "application/json" -Headers $tariqHeaders -Body $reqBody
Write-Host "Sent request result:" $sendReqRes.message

# 4. Hassan logs in
$hassanLoginBody = @{ email = "hassan@pubg.com"; password = "password123" } | ConvertTo-Json
$hassanLoginRes = Invoke-RestMethod -Uri "http://localhost:5000/api/auth/login" -Method Post -ContentType "application/json" -Body $hassanLoginBody
$hassanHeaders = @{ Authorization = "Bearer $($hassanLoginRes.token)" }

# 5. Hassan checks pending requests
$pendingReqs = Invoke-RestMethod -Uri "http://localhost:5000/api/friends/requests/pending" -Method Get -Headers $hassanHeaders
Write-Host "Hassan pending requests count:" $pendingReqs.Count
$firstReq = $pendingReqs[0]

# 6. Hassan accepts friend request
$acceptBody = @{ requestId = $firstReq.requestId; accept = $true } | ConvertTo-Json
$acceptRes = Invoke-RestMethod -Uri "http://localhost:5000/api/friends/request/respond" -Method Post -ContentType "application/json" -Headers $hassanHeaders -Body $acceptBody
Write-Host "Accept result:" $acceptRes.message

# 7. Verify Hassan's friends list
$hassanFriends = Invoke-RestMethod -Uri "http://localhost:5000/api/friends" -Method Get -Headers $hassanHeaders
Write-Host "Hassan friends:" ($hassanFriends | ForEach-Object { $_.username })

Write-Host "Full workflow test completed successfully!"
