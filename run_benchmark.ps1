$ErrorActionPreference = "Stop"
Write-Host "=== Building JChess ===" -ForegroundColor Cyan
$outDir = "target/classes"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}
$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -d $outDir $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "=== Running Benchmark ===" -ForegroundColor Cyan
    & java -server -XX:+UseCompressedOops -cp "$outDir;." com.jchess.BotBenchmark
} else {
    Write-Error "Build failed"
}
