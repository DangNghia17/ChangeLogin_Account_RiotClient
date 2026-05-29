<#
.SYNOPSIS
    Builds the Riot Account Manager runnable JAR (Windows / PowerShell).

.DESCRIPTION
    Prefers Maven if available; otherwise falls back to a plain javac build that
    produces a self-contained "fat" JAR (with the org.json dependency bundled).

    Output: dist\RiotAccountManager.jar

.NOTES
    Requires JDK 11+ on PATH (javac, jar). Maven is optional.
#>
param(
    [string]$JsonVersion = "20231013"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$distDir = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

function Build-WithMaven {
    Write-Host "Maven found - building with 'mvn package'..." -ForegroundColor Cyan
    mvn -q -DskipTests clean package
    $shaded = Get-ChildItem -Path (Join-Path $root "target") -Filter "riot-account-manager-*.jar" |
        Where-Object { $_.Name -notlike "*original*" } | Select-Object -First 1
    if ($null -eq $shaded) { throw "Maven build did not produce a JAR." }
    Copy-Item $shaded.FullName (Join-Path $distDir "RiotAccountManager.jar") -Force
}

function Build-WithJavac {
    Write-Host "Maven not found - building with javac..." -ForegroundColor Cyan

    $libDir = Join-Path $root "lib"
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    $jsonJar = Join-Path $libDir "json-$JsonVersion.jar"
    if (-not (Test-Path $jsonJar)) {
        $url = "https://repo1.maven.org/maven2/org/json/json/$JsonVersion/json-$JsonVersion.jar"
        Write-Host "Downloading dependency: $url"
        Invoke-WebRequest -Uri $url -OutFile $jsonJar
    }

    $classes = Join-Path $root "build\classes"
    if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
    New-Item -ItemType Directory -Force -Path $classes | Out-Null

    $sources = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter "*.java" |
        ForEach-Object { $_.FullName }
    & javac --release 11 -encoding UTF-8 -cp $jsonJar -d $classes $sources

    Copy-Item (Join-Path $root "src\main\resources\*") $classes -Recurse -Force

    # Bundle org.json into the classes dir to create a fat JAR.
    Push-Location $classes
    & jar xf $jsonJar
    if (Test-Path "META-INF\MANIFEST.MF") { Remove-Item "META-INF\MANIFEST.MF" -Force }
    Pop-Location

    $jar = Join-Path $distDir "RiotAccountManager.jar"
    & jar cfe $jar "com.riotaccountmanager.App" -C $classes .
}

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Build-WithMaven
} else {
    Build-WithJavac
}

Write-Host "Build complete: $distDir\RiotAccountManager.jar" -ForegroundColor Green
