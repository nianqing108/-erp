$b = "http://localhost:8080/api"
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"
Write-Host "--- orders list ---"
curl.exe -s "$b/orders?pageNum=1&pageSize=10" -H $H
Write-Host ""
Write-Host "--- order 1 detail ---"
curl.exe -s "$b/orders/1" -H $H
Write-Host ""
Write-Host "--- dashboard raw ---"
curl.exe -s "$b/dashboard/overview" -H $H
