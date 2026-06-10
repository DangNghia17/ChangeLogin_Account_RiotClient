<#
.SYNOPSIS
    Packages Riot Account Manager into a self-contained app using jpackage.

.DESCRIPTION
    Produces two artifacts so the end user never has to install Java:

      1. Portable  : dist\RiotAccountManager (app-image, runtime bundled) + a .zip
      2. Installer : dist\RiotAccountManager-<ver>.exe  (Setup.exe; requires WiX Toolset)

    The bundled runtime is created automatically by jpackage via jlink, so the
    download is small and Java-free for the user.

.NOTES
    Requires JDK 17+ (jpackage). The .exe/.msi installer types additionally require
    the WiX Toolset (https://wixtoolset.org). The portable app-image needs no WiX.
#>
param(
    [string]$AppVersion = "2.0.0",
    [switch]$SkipInstaller
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# 1) Ensure the JAR exists.
& (Join-Path $PSScriptRoot "build.ps1")

$distDir = Join-Path $root "dist"
$jar = Join-Path $distDir "RiotAccountManager.jar"
if (-not (Test-Path $jar)) { throw "JAR not found: $jar" }

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage not found. Install a JDK 17+ and ensure jpackage is on PATH."
}

$inputDir = Join-Path $distDir "jpackage-input"
if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item $jar $inputDir -Force

$iconArg = @()
$icon = Join-Path $root "dist\app-icon.ico"
if (Test-Path $icon) { $iconArg = @("--icon", $icon) }

$commonArgs = @(
    "--name", "RiotAccountManager",
    "--app-version", $AppVersion,
    "--input", $inputDir,
    "--main-jar", "RiotAccountManager.jar",
    "--main-class", "com.riotaccountmanager.App",
    "--vendor", "Riot Account Manager",
    "--java-options", "-Dsun.java2d.uiScale.enabled=true"
) + $iconArg

# 2) Portable app-image (no WiX needed).
$portableOut = Join-Path $distDir "portable"
if (Test-Path $portableOut) { Remove-Item -Recurse -Force $portableOut }
New-Item -ItemType Directory -Force -Path $portableOut | Out-Null

Write-Host "Building portable app-image..." -ForegroundColor Cyan
& jpackage @commonArgs --type app-image --dest $portableOut

$zip = Join-Path $distDir "RiotAccountManager-$AppVersion-portable.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $portableOut "RiotAccountManager\*") -DestinationPath $zip
Write-Host "Portable package: $zip" -ForegroundColor Green

# 3) Installer (Setup.exe) - requires WiX.
if (-not $SkipInstaller) {
    Write-Host "Building Windows installer (requires WiX Toolset)..." -ForegroundColor Cyan
    try {
        & jpackage @commonArgs `
            --type exe `
            --dest $distDir `
            --win-dir-chooser `
            --win-menu `
            --win-shortcut `
            --win-shortcut-prompt
        Write-Host "Installer created in $distDir" -ForegroundColor Green
    } catch {
        Write-Warning "Installer build failed (is WiX installed?). Portable package is still available."
        Write-Warning $_.Exception.Message
    }
}

Write-Host "Done." -ForegroundColor Green
