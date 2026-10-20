param(
    [Parameter(Mandatory = $true)][string]$RepoUrl,
    [string]$Branch = "main",
    [string]$Message = "lio01 Android client v1.0.1"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 git，请先安装 Git for Windows: https://git-scm.com/download/win"
    exit 1
}

if (-not (Test-Path ".git")) {
    git init | Out-Null
    Write-Host "已初始化本地仓库"
}

git add -A
git diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    git commit -m $Message | Out-Null
    Write-Host "已提交: $Message"
} else {
    Write-Host "没有新的改动需要提交"
}

git branch -M $Branch

$remotes = git remote
if ($remotes -contains "origin") {
    git remote set-url origin $RepoUrl
} else {
    git remote add origin $RepoUrl
}

git push -u origin $Branch
Write-Host "完成：$RepoUrl"
