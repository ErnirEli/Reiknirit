# How to read the saved results

These files are records of earlier runs. Comments belong here rather than inside
the CSV files, so the original observations stay intact and remain machine-readable.

| File | What it contains |
|---|---|
| `full.csv` | Initial seven-size experiment: 408 observations, including six failures. |
| `tuned.csv` | Retest after changing the JVM memory layout: 120 successful observations. |
| `full-summary.md` | Median, minimum, maximum and failed counts grouped from `full.csv`. |
| `tuned-summary.md` | The same summary for `tuned.csv`. |
| `full.csv.environment.txt` | Date, Java version, OS, processor counts, memory settings and arguments for the initial run. |
| `tuned.csv.environment.txt` | Environment for the retest, including `NewRatio=8`. |
| `correctness.txt` | Saved output of the exact correctness suite: 4,452 comparisons passed. |

## CSV columns

Each row represents one attempted measurement. The first line names the columns.

| Column | Meaning |
|---|---|
| `method` | `radix8`, `radix11`, `radix16`, `jdk` or `parallel`. |
| `pattern` | `sorted`, `partial`, `random` or `reverse` input. |
| `n` | Number of `long` elements, each with an 8-byte payload. |
| `trial` | Repetition index starting at zero; defaults are 0, 1 and 2. |
| `seed` | Number initializing the random generator; matching inputs allow fair comparisons. |
| `setup_ms` | Time spent allocating and generating the measured input, in milliseconds. |
| `sort_ms` | Sorting time only; blank when a usable measurement was not reported. |
| `status` | Outcome, as described below. |

| Status | Meaning |
|---|---|
| `OK` | Sort completed within its cap and passed validation. |
| `SETUP_CAP` | Input generation took over 15 seconds; the measured sort was not started. |
| `SORT_CAP` | The sort exceeded its deadline or its measured duration exceeded 15 seconds. |
| `PROCESS_TIMEOUT` | The controller's 90-second deadline expired without a reported row. |
| `ERROR_EXIT_<code>` | Worker exited without a row; inspect its console error for the cause. |

A failure stops later attempts for that method/pattern pair, including larger
sizes. Skipped attempts produce no rows and are not counted in summary failures.
Blank timing cells mean missing measurements, not zero and not exactly 15 seconds.

## Summary tables and environment

Successful trials alone contribute to median/min/max. For times 8, 3 and 4 ms,
sort them to 3, 4 and 8: median = 4, minimum = 3, maximum = 8. For an even number,
average the two middle values. An em dash means there were no successful times.
Table order follows the first appearance of a case in the shuffled CSV.

Environment files use `name=value` lines. `hostProcessors` is the processor count
visible to the controller; `workerProcessors` is the configured count exposed to
each child JVM. `heap` describes managed Java memory; code cache holds compiled
code, metaspace holds class metadata and stacks hold thread call frames.
MiB and KiB mean 2^20 and 2^10 bytes. `NewRatio=8` requests an old-to-young
generation ratio of 8:1. See the main README for total-memory limitations.

Compare methods within the same experiment. Do not combine the initial and tuned
runs into one median because their garbage collector configurations differed.
