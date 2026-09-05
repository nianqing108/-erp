$b = "http://localhost:8080/api"
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"
$r = curl.exe -s -X POST "$b/orders/2/pay" -H "Content-Type: application/json" -H "$H" -d "@smoke/payfull.json"
Write-Host "pay: $r"
$d = (curl.exe -s "$b/orders/2" -H "$H" | ConvertFrom-Json).data
Write-Host "order2 status=$($d.status) label=$($d.statusLabel) paidRatio=$($d.paidRatio) actions=$($d.availableActions -join ',')"
