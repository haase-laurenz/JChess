# JChess Tournament Runner Skript
# Kompiliert das Projekt und startet ein automatisiertes Schachturnier zwischen verschiedenen Engines

$ErrorActionPreference = "Stop"

Write-Host "=== JChess Tournament Build & Run ===" -ForegroundColor Cyan

$outDir = "target/classes"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

# 1. Ressourcen kopieren
if (Test-Path "src/main/resources") {
    Copy-Item -Path "src/main/resources/*" -Destination $outDir -Recurse -Force -ErrorAction SilentlyContinue
}

# 2. Alle Java-Dateien suchen und kompilieren
$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Host "Kompiliere $($javaFiles.Count) Java-Quelldateien..." -ForegroundColor Yellow
& javac -encoding UTF-8 -d $outDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Kompilierung fehlgeschlagen!"
    exit $LASTEXITCODE
}

Write-Host "Kompilierung erfolgreich! Starte Turnier..." -ForegroundColor Green

# 3. Turnier mit optimierten JVM-Argumenten starten
& java -server -Xms2G -Xmx4G -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -cp "$outDir;." com.jchess.tournament.TournamentRunner
