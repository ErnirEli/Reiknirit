# Run from PowerShell: ./run.ps1 quick (or full for the larger size grid).
# This compiles, checks correctness, measures performance and creates a table.
# Existing results for the selected mode are overwritten.
param([ValidateSet('quick', 'full')][string]$Mode = 'quick')
# Stop on PowerShell errors. Native Java commands also need exit-code checks below.
$ErrorActionPreference = 'Stop'
# Resolve relative paths from this script's folder, whatever the caller's folder.
Push-Location $PSScriptRoot
try {
    # -Force permits these directories to exist; Out-Null hides directory listings.
    New-Item -ItemType Directory -Force build, results | Out-Null
    # Compile all source files. -Xlint:all enables compiler warnings; -d selects output.
    $sortSources = Get-ChildItem -LiteralPath src -Filter '*.java' | ForEach-Object FullName
    & javac -Xlint:all -d build $sortSources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    # -cp tells Java where the compiled classes are; -Xmx caps the Java heap.
    & java -Xmx640m -cp build SortTests
    if ($LASTEXITCODE -ne 0) { throw 'Correctness tests failed' }
    # The controller uses a small heap and launches its own 640 MiB workers.
    & java -Xmx64m -cp build Benchmark $Mode "results/$Mode.csv"
    if ($LASTEXITCODE -ne 0) { throw 'Benchmark failed' }
    # Pipe the printed Markdown table into a UTF-8 file.
    & java -cp build Summarize "results/$Mode.csv" | Set-Content -Encoding utf8 "results/$Mode-summary.md"
    if ($LASTEXITCODE -ne 0) { throw 'Summary failed' }
} finally {
    # Restore the caller's working directory even if a command failed.
    Pop-Location
}
