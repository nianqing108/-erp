$ErrorActionPreference = "Stop"
$b = "http://localhost:8080/api"

# fresh H2 -> register + login
$r = curl.exe -s -X POST "$b/auth/register" -H "Content-Type: application/json" -d "@smoke/reg.json"
Write-Host "register: $r"
$token = (curl.exe -s -X POST "$b/auth/login" -H "Content-Type: application/json" -d "@smoke/login.json" | ConvertFrom-Json).data.token
$H = "Authorization: Bearer $token"

curl.exe -s -X POST "$b/customers" -H "Content-Type: application/json" -H "$H" -d "@smoke/cust.json" | Out-Null

# order A: dueDate 2026-09-15 (this month) 6000 -> monthDueAmount
$a = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d '{"customerId":1,"orderDate":"2026-09-05","totalAmount":6000,"dueDate":"2026-09-15"}'
$idA = ($a | ConvertFrom-Json).data.id
curl.exe -s -X POST "$b/orders/$idA/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship2.json" | Out-Null
Write-Host "orderA shipped id=$idA (dueDate this month, 6000)"

# order B: dueDate 2026-10-10 (next month) 3000 -> notDueAmount
$bb = curl.exe -s -X POST "$b/orders" -H "Content-Type: application/json" -H "$H" -d '{"customerId":1,"orderDate":"2026-09-05","totalAmount":3000,"dueDate":"2026-10-10"}'
$idB = ($bb | ConvertFrom-Json).data.id
curl.exe -s -X POST "$b/orders/$idB/ship" -H "Content-Type: application/json" -H "$H" -d "@smoke/ship2.json" | Out-Null
Write-Host "orderB shipped id=$idB (dueDate next month, 3000)"

$dash = (curl.exe -s "$b/dashboard/overview" -H "$H" | ConvertFrom-Json).data
Write-Host ("monthDueAmount={0}  notDueAmount={1}  receivable={2}" -f $dash.monthDueAmount, $dash.notDueAmount, $dash.receivable)
