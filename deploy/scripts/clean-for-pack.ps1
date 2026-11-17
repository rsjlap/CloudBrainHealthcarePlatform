# 打包交付前运行：删除可再生的构建产物，显著减小压缩包体积
# 用法: .\deploy\scripts\clean-for-pack.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

$targets = @(
    (Join-Path $Root "cloud-brain-medical-frontend\node_modules"),
    (Join-Path $Root "cloud-brain-medical-frontend\dist"),
    (Join-Path $Root "cloud-brain-medical-backend\target"),
    (Join-Path $Root "deploy\nginx\html")
)

foreach ($path in $targets) {
    if (-not (Test-Path $path)) { continue }
    if ($path -like "*\deploy\nginx\html") {
        Get-ChildItem $path -Exclude ".gitkeep" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "cleared: $path (kept .gitkeep)"
    } else {
        Remove-Item $path -Recurse -Force
        Write-Host "removed: $path"
    }
}

Write-Host ""
Write-Host "Done. Zip the job/ folder now (~1-5 MB source)." -ForegroundColor Green
Write-Host "Recipient runs: npm install (frontend) + mvn package (backend)" -ForegroundColor Yellow
