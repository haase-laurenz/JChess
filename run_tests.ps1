# JChess Test Runner
$ErrorActionPreference = "Stop"

$outDir = "target/classes"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
Write-Host "Kompiliere $($javaFiles.Count) Dateien..." -ForegroundColor Yellow
& javac -encoding UTF-8 -d $outDir $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Error "Kompilierung fehlgeschlagen!"
    exit $LASTEXITCODE
}

Write-Host "Starte TestRunner..." -ForegroundColor Cyan
& java -cp "$outDir;." com.jchess.TestRunner
