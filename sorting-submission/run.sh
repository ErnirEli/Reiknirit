#!/bin/sh
# Run with sh run.sh quick, or use full for the larger size grid.
# Existing results for the selected mode are overwritten.
# -e stops on a failed command; -u rejects unset variables.
set -eu
# Work relative to the script, even when invoked from another directory.
cd "$(dirname "$0")"
# Use the first argument, or quick if it was omitted or empty.
mode=${1:-quick}
case "$mode" in
    quick|full) ;;
    *)
        echo 'Usage: sh run.sh [quick|full]' >&2
        exit 2
        ;;
esac
# -p allows existing folders. Compile source into build with warnings enabled.
mkdir -p build results
javac -Xlint:all -d build src/*.java
# -cp locates compiled classes; -Xmx sets a heap cap, not total process RAM.
java -Xmx640m -cp build SortTests
# The controller launches a separate, bounded-heap worker for each measurement.
java -Xmx64m -cp build Benchmark "$mode" "results/$mode.csv"
# Redirect the summary's standard output into a Markdown table file.
java -cp build Summarize "results/$mode.csv" > "results/$mode-summary.md"
