$ErrorActionPreference = "Stop"
$b = "http://localhost:8080/api"

curl.exe -s -X POST "$b/auth/register" -H "Content-Type: application/json" -d "@smoke/reg.json" | Out-Null
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"

curl.exe -s -X POST "$b/customers" -H "Content-Type: application/json" -H "$H" -d "@smoke/cust.json" | Out-Null

# order 1: dueDate 2026-07-01 -> overdue ~66 days
$o1 = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d "@smoke/order_overdue.json"
$id1 = ($o1 | ConvertFrom-Json).data.id
$body1 = '{"shipmentDate":"2026-08-01","trackingNo":"SF-A"}'
curl.exe -s -X POST "$b/orders/$id1/ship" -H "Content-Type: application/json" -H "$H" -d $body1 | Out-Null
Write-Host "order1 shipped id=$id1"

# order 2: dueDate 2026-09-20 -> not due
$o2 = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d "@smoke/order_notdue.json"
$id2 = ($o2 | ConvertFrom-Json).data.id
$body2 = '{"shipmentDate":"2026-09-05"}'
curl.exe -s -X POST "$b/orders/$id2/ship" -H "Content-Type: application/json" -H "$H" -d $body2 | Out-Null
Write-Host "order2 shipped id=$id2"

$dash = (curl.exe -s "$b/dashboard/overview" -H "$H" | ConvertFrom-Json).data
Write-Host "--- aging buckets ---"
$dash.aging | ForEach-Object { Write-Host ("{0} : {1} yuan / {2} orders" -f $_.label, $_.amount, $_.count) }
Write-Host ("receivable={0} monthOrderAmount={1}" -f $dash.receivable, $dash.monthOrderAmount)
