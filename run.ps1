# JChess Windows Start- und Build-Skript
# Kompiliert das Projekt mit JDK 21 und startet die Benutzeroberfläche

$ErrorActionPreference = "Stop"

Write-Host "=== JChess Build & Run ===" -ForegroundColor Cyan

$outDir = "target/classes"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

# 1. Ressourcen kopieren
if (Test-Path "src/main/resources") {
    Copy-Item -Path "src/main/resources/*" -Destination $outDir -Recurse -Force -ErrorAction SilentlyContinue
}

# 2. Alle Java-Dateien suchen
$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

if ($javaFiles.Count -eq 0) {
    Write-Error "Keine Java-Quelldateien in src/main/java gefunden!"
    exit 1
}

Write-Host "Kompiliere $($javaFiles.Count) Java-Quelldateien..." -ForegroundColor Yellow
& javac -encoding UTF-8 -d $outDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Kompilierung fehlgeschlagen!"
    exit $LASTEXITCODE
}

Write-Host "Kompilierung erfolgreich!" -ForegroundColor Green
Write-Host "Starte JChess GUI..." -ForegroundColor Cyan

# 3. Programm mit optimierten JVM-Argumenten starten
& java -server -Xms2G -Xmx4G -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -cp "$outDir;." com.jchess.Main
