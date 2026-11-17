# 一键构建前后端产物（Windows PowerShell）
$ErrorActionPreference = "Stop"

$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Frontend = Join-Path $Root "cloud-brain-medical-frontend"
$Backend = Join-Path $Root "cloud-brain-medical-backend"
$Out = Join-Path $Root "deploy\artifacts"

Write-Host "==> 构建前端..." -ForegroundColor Cyan
Push-Location $Frontend
npm ci
npm run build
Pop-Location

Write-Host "==> 构建后端 Jar..." -ForegroundColor Cyan
Push-Location $Backend
mvn -q clean package -DskipTests
Pop-Location

$FrontendOut = Join-Path $Out "frontend"
$BackendOut = Join-Path $Out "backend"
New-Item -ItemType Directory -Force -Path $FrontendOut, $BackendOut | Out-Null
Remove-Item -Recurse -Force (Join-Path $FrontendOut "*") -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force (Join-Path $Frontend "dist\*") $FrontendOut
$Jar = Get-ChildItem (Join-Path $Backend "target\cloud-brain-medical-backend-*.jar") | Select-Object -First 1
Copy-Item $Jar.FullName (Join-Path $BackendOut "app.jar")

Write-Host ""
Write-Host "构建完成：" -ForegroundColor Green
Write-Host "  前端静态资源: $FrontendOut"
Write-Host "  后端 Jar:       $(Join-Path $BackendOut 'app.jar')"
