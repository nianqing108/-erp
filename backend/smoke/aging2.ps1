$b = "http://localhost:8080/api"
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"

$r1 = curl.exe -s -X POST "$b/orders/1/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship1.json"
Write-Host "ship1: $r1"
$r2 = curl.exe -s -X POST "$b/orders/2/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship2.json"
Write-Host "ship2: $r2"

$dash = (curl.exe -s "$b/dashboard/overview" -H "$H" | ConvertFrom-Json).data
Write-Host "--- aging buckets ---"
$dash.aging | ForEach-Object { Write-Host ("{0} : {1} yuan / {2} orders" -f $_.label, $_.amount, $_.count) }
Write-Host ("receivable={0}" -f $dash.receivable)
