# JChess Perft Benchmark & Verifikations-Skript
param(
    [int]$Depth = 6
)

$ErrorActionPreference = "Stop"

Write-Host "=== JChess Perft Test Runner ===" -ForegroundColor Cyan
Write-Host "Ausführung bis Tiefe $Depth Ply..." -ForegroundColor Yellow

# 1. Kompilieren falls nötig
$outDir = "target/classes"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d $outDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Kompilierung fehlgeschlagen!"
    exit $LASTEXITCODE
}

# 2. Perft ausführen (mit JVM-Optimierungen für maximale Engine-Performance)
& java -server -XX:+UseCompressedOops -cp "$outDir;." com.jchess.PerftRunner $Depth

exit $LASTEXITCODE
