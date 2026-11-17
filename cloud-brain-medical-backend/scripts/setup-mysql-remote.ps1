# Run as Administrator in PowerShell:
#   cd cloud-brain-medical-backend
#   .\scripts\setup-mysql-remote.ps1

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== Cloud Brain Medical - MySQL Remote Setup ===" -ForegroundColor Cyan

$ruleName = "MySQL Cloud Brain 3306"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[SKIP] Firewall rule already exists: $ruleName" -ForegroundColor Yellow
} else {
    try {
        New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort 3306 -Action Allow | Out-Null
        Write-Host "[OK] Inbound TCP 3306 allowed" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] Firewall rule needs Administrator. Add TCP 3306 manually in wf.msc" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Your IPv4 (teammates put this in DB_URL):" -ForegroundColor Cyan
Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -notlike '127.*' -and $_.PrefixOrigin -ne 'WellKnown' } |
    ForEach-Object { Write-Host "  -> $($_.IPAddress)" -ForegroundColor Green }

Write-Host @"

NEXT STEPS (manual):

1) Edit MySQL my.ini (often):
   C:\ProgramData\MySQL\MySQL Server 8.0\my.ini
   Under [mysqld] add or set:  bind-address = 0.0.0.0
   Then restart MySQL service (services.msc).

2) Create remote user - run in Navicat or:
   mysql -u root -p < src\main\resources\db\remote-access.sql

3) YOU (MySQL host): keep DB_URL as localhost in application-dev.yml

4) TEAMMATE copies .env.team.example and sets:
   DB_URL=jdbc:mysql://YOUR_IP:3306/cloud_brain_medical?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
   DB_USER=cloud_team
   DB_PASSWORD=cloud123456

5) Teammate tests connection with Navicat to YOUR_IP:3306
"@ -ForegroundColor White
