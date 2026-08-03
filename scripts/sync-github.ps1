param(
  [Parameter(Mandatory = $true)]
  [string]$Message,
  [string]$Version,
  [string]$ApkPath
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

git add --all
git commit -m $Message
git push origin main

if ($Version -and $ApkPath) {
  if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "源码已同步，但发布 Release 需要先安装并登录 GitHub CLI。"
  }
  $resolvedApk = (Resolve-Path $ApkPath).Path
  gh release create "v$Version" $resolvedApk --title "爱拼豆 v$Version" --generate-notes
}
