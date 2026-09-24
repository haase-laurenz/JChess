# JChess Windows Build-Skript (Headless-Kompilierung)
$ErrorActionPreference = "Stop"

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

Write-Host "Kompilierung erfolgreich!" -ForegroundColor Green
