$ErrorActionPreference = "Stop"
$b = "http://localhost:8080/api"
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"

$a = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d "@smoke/orderA.json"
Write-Host "createA: $a"
$idA = ($a | ConvertFrom-Json).data.id
curl.exe -s -X POST "$b/orders/$idA/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship2.json" | Out-Null

$bb = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d "@smoke/orderB.json"
Write-Host "createB: $bb"
$idB = ($bb | ConvertFrom-Json).data.id
curl.exe -s -X POST "$b/orders/$idB/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship2.json" | Out-Null

$dash = (curl.exe -s "$b/dashboard/overview" -H "$H" | ConvertFrom-Json).data
Write-Host ("monthDueAmount={0}  notDueAmount={1}  receivable={2}" -f $dash.monthDueAmount, $dash.notDueAmount, $dash.receivable)
