$base = "http://localhost:8080/api"

function Post($url, $file) { curl.exe -s -X POST "$url" -H "Content-Type: application/json" -d "@$file" }
function Get($url) { curl.exe -s "$url" }

$name = "冒烟客户_" + (Get-Date -Format "yyyyMMddHHmmss")
$cust = (Get-Content smoke/cust.json) -replace '"测试客户A"', ('"' + $name + '"')
Set-Content smoke/cust_run.json $cust
$c = Post "$base/customers" "smoke/cust_run.json"
Write-Host "createCustomer: $c"
$cid = ($c | ConvertFrom-Json).data
Write-Host "customerId=$cid"

$ojson = (Get-Content smoke/order.json) -replace "__CID__", $cid
Set-Content smoke/order_run.json $ojson
$o = Post "$base/orders" "smoke/order_run.json"
Write-Host "createOrder: $o"
$oid = ($o | ConvertFrom-Json).data.id
Write-Host "orderId=$oid"

Write-Host "ship: $(Post "$base/orders/$oid/ship" smoke/ship.json)"
Write-Host "confirmShip: $(Post "$base/orders/$oid/confirm-ship" smoke/confirmship.json)"
Write-Host "pay1: $(Post "$base/orders/$oid/pay" smoke/pay1.json)"
Write-Host "pay2: $(Post "$base/orders/$oid/pay" smoke/pay2.json)"
Write-Host "payOver(expect fail): $(Post "$base/orders/$oid/pay" smoke/pay_over.json)"
Write-Host "pay3: $(Post "$base/orders/$oid/pay" smoke/pay3.json)"
Write-Host "detail: $(Get "$base/orders/$oid")"
Write-Host "dashboard: $(Get "$base/dashboard/overview")"
Write-Host "statement: $(Get "$base/reports/statement/$cid`?from=2026-09-01`&to=2026-09-30")"
Write-Host "monthly: $(Get "$base/reports/monthly`?month=2026-09")"
