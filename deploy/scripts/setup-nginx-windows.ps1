param(
    [string]$NginxHome = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$Frontend = Join-Path $Root "cloud-brain-medical-frontend"
$DeployNginx = Join-Path $Root "deploy\nginx"
$HtmlDir = Join-Path $DeployNginx "html"
$Template = Join-Path $DeployNginx "cloud-brain-medical.conf.template"
$Generated = Join-Path $DeployNginx "cloud-brain-medical.generated.conf"
$MainConfExample = Join-Path $DeployNginx "nginx.conf.example"

if (-not $SkipBuild) {
    Write-Host "==> build frontend..." -ForegroundColor Cyan
    Push-Location $Frontend
    if (-not (Test-Path "node_modules")) { npm ci }
    npm run build
    Pop-Location
}

Write-Host "==> sync dist to deploy/nginx/html ..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $HtmlDir | Out-Null
Get-ChildItem $HtmlDir -Exclude ".gitkeep" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force (Join-Path $Frontend "dist\*") $HtmlDir

$HtmlRoot = ($HtmlDir -replace '\\', '/')

Write-Host "==> generate nginx site config..." -ForegroundColor Cyan
$conf = Get-Content $Template -Raw -Encoding UTF8
$conf = $conf.Replace('root __NGINX_HTML_ROOT__;', "root $HtmlRoot;")
[System.IO.File]::WriteAllText($Generated, $conf, [System.Text.UTF8Encoding]::new($false))

Write-Host ""
Write-Host "Done:" -ForegroundColor Green
Write-Host "  static files: $HtmlDir"
Write-Host "  site config:  $Generated"
Write-Host "  nginx root:   $HtmlRoot"

if ($NginxHome) {
    $NginxConfDir = Join-Path $NginxHome "conf"
    if (-not (Test-Path $NginxConfDir)) {
        $nested = Get-ChildItem $NginxHome -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName "nginx.exe") } |
            Select-Object -First 1
        if ($nested) {
            $NginxHome = $nested.FullName
            $NginxConfDir = Join-Path $NginxHome "conf"
            Write-Host "Detected nested nginx dir: $NginxHome" -ForegroundColor Yellow
        }
    }
    if (-not (Test-Path $NginxConfDir)) {
        throw "nginx conf dir not found: $NginxConfDir (use -NginxHome pointing to folder containing nginx.exe)"
    }

    Copy-Item -Force $Generated (Join-Path $NginxConfDir "cloud-brain-medical.generated.conf")
    Copy-Item -Force $MainConfExample (Join-Path $NginxConfDir "nginx.conf")

    Push-Location $NginxHome
    & .\nginx.exe -t
    if ($LASTEXITCODE -ne 0) { throw "nginx -t failed" }
    Write-Host ""
    Write-Host "Installed to $NginxConfDir" -ForegroundColor Green
    Write-Host "Start:  cd $NginxHome; start nginx" -ForegroundColor Yellow
    Write-Host "Reload: nginx -s reload" -ForegroundColor Yellow
    Write-Host "Stop:   nginx -s stop" -ForegroundColor Yellow
    Pop-Location
} else {
    Write-Host ""
    Write-Host "Next steps (Windows):" -ForegroundColor Yellow
    Write-Host "  1. Download nginx: https://nginx.org/en/download.html"
    Write-Host "  2. Extract to e.g. C:\nginx"
    Write-Host "  3. Re-run with -NginxHome C:\nginx"
    Write-Host "  4. Start backend jar on port 8081 (spring.profiles.active=prod)"
    Write-Host "  5. Open http://localhost"
}
