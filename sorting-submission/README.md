# Signed 64-bit sorting experiment

**Start with [WALKTHROUGH.md](WALKTHROUGH.md)** for a beginner-friendly reading order,
a worked radix-sort example, Java syntax explanations and a guide to every file.
The source files also explain each stage beside the code. This README is the
detailed reference for running the experiment and interpreting its limits.

This submission interprets the assignment's “n-bit array” as **n elements**, each a signed 64-bit Java `long`. It supplies an adaptive least-significant-digit radix sorter, exact correctness tests, and a reproducible comparison with `Arrays.sort(long[])` and `Arrays.parallelSort(long[])`. No external libraries are required. JDK 17 or later is required; the included measurements use JDK 21.

## Compile and run

One-command option: `./run.ps1 quick` in PowerShell, or `sh run.sh quick` on macOS/Linux. Substitute `full` for the complete size grid. These scripts compile, test, benchmark and summarize. Manual commands follow.

From this folder, on Windows PowerShell, macOS or Linux:

```text
mkdir build
javac -Xlint:all -d build src/FastSort.java src/SortSupport.java src/SortTests.java src/Benchmark.java src/Summarize.java
java -Xmx640m -cp build SortTests
java -Xmx64m -cp build Benchmark quick results/quick.csv
java -Xmx64m -cp build Benchmark full results/full.csv
java -cp build Summarize results/full.csv
```

The archive already contains the `build` and `results` directories; skip `mkdir build` if it exists. Recompile on your own computer rather than relying on supplied class files. The full experiment is many separate sorts and can take several minutes. **15 seconds applies to each sorting invocation, not the entire experiment.**

Arguments: `Benchmark [quick|full] [output.csv] [repeats] [comma-separated methods] [comma-separated sizes]`.

```text
java -Xmx64m -cp build Benchmark full results/custom.csv 3 radix8,radix11,radix16,jdk,parallel 1000000,4000000,16000000,30000000
```

The output parent directory must exist. Existing results at the specified path are overwritten. Defaults use three independent JVM trials per case, two sizes for quick, and seven sizes up to 30 million for full. Each trial index uses the same seed across algorithms. Algorithm/pattern execution order is shuffled reproducibly. CSV contains individual observations; compare medians, minimum and maximum, not just the best run. Record your CPU model, power settings, background load and OS memory measurements with your report. Environment sidecars record JVM, OS, processor count and invocation arguments.

## Use the sorter

```java
long[] values = {Long.MAX_VALUE, -1, 0, Long.MIN_VALUE};
FastSort.sort(values); // ascending signed order, modifies the array
FastSort.radixSort(values, 8); // explicit width: 8, 11 or 16
```

The default is 8 bits, selected after it achieved the best median among the tested radix variants on large random and partially sorted inputs in the final retest. This is not a claim of universal optimality. Use the included empirical results to assess this choice on the tested machine and repeat on the grading machine. Tiny inputs below 2,048 elements use the library sort. An ascending scan permits immediate return; a descending scan permits in-place reversal, including duplicates. Scans stop as soon as both possibilities are excluded. These optimizations apply equally to all radix widths and are included in timing.

Otherwise, each pass counts digit frequencies, forms exclusive prefix sums, and stably scatters into a second `long[]`. The key `value ^ Long.MIN_VALUE` maps signed order to unsigned order by flipping the sign bit. Unsigned shifts extract digits; the last digit uses only the remaining bits (nine for the 11-bit variant). Inductively, stable scattering preserves the ordering by all previously processed lower digits; after the last pass all 64 bits are ordered. Values themselves remain unchanged. Constant digits are skipped without swapping buffers. A final copy occurs if skipped passes leave the result in the temporary buffer.

Widths 8, 11 and 16 need at most 8, 6 and 4 passes, respectively, with 256, 2,048 and 65,536 histogram slots. Fewer passes do not guarantee better speed: large buckets produce scattered writes and greater cache/TLB pressure. Complexity is O(ceil(64/b) * (n + 2^b)), or O(n) for fixed width, with O(n + 2^b) auxiliary space. Sorted and reverse inputs take O(n) time and O(1) extra space. This is an optimized sequential implementation, not an exhaustive search over parallel radix, native code or every possible algorithm.

## Timing and correctness

Each observation runs in a new JVM with identical memory settings. Sixteen 100,000-element warm-up sorts cover all four patterns before measurement. Warm-up reduces first-use effects but cannot guarantee complete JIT stabilization at every size. Fresh forks avoid accumulating earlier large arrays. A requested garbage collection occurs outside timing; collections and auxiliary allocations *during* the sort remain part of its cost. No temporary sorting buffer is allocated before timing.

Input generation is measured separately. `System.nanoTime()` immediately surrounds the sorting dispatch and includes algorithm selection, ordered scans, buffer allocation, counting, scattering and any final copy. It excludes input generation, pre-sort fingerprints, warm-up, result verification, console/CSV output and JVM startup. The tiny dispatch overhead is shared across methods. Validation consumes the actual output.

Patterns are: evenly spaced values spanning signed range in ascending order; the same values with n/100 random pair swaps (partial); pseudorandom full-width `nextLong()` values; and descending values. Partial order is precisely defined and is only one model of partially sorted data. Seeds vary between trials. Sorted patterns and random patterns have different value distributions; compare algorithms on the same pattern.

`SortTests` compares every element with an independently library-sorted clone, for all algorithms and the public default. It covers empty/singleton arrays, threshold boundaries, extremes, duplicates, equal values, all 64 individual bits, skipped passes, and seeded random cases. Large benchmark inputs receive a full signed-order scan and three order-independent 64-bit fingerprints (sum, mixed sum, mixed XOR) before/after. These are probabilistic multiset checks, **not an exact proof of permutation**; exact tests complement them without retaining a third full-size array.

## Resource limits and scaling

Workers use `-Xmx640m`, Serial GC with `-XX:NewRatio=8`, a 64 MiB code-cache cap, 96 MiB metaspace cap, 512 KiB thread stacks and four active processors. The parallel library baseline may use multiple threads; label it as such when comparing against sequential algorithms. The controller uses `-Xmx64m` in the commands above and runs workers sequentially. The larger old generation allows both large radix arrays to fit: total free heap alone is insufficient when a collector partitions it into generations.

At 30 million elements, input plus radix scratch payload is 480,000,000 bytes (about 458 MiB). The largest histogram is 262,144 bytes; headers and other heap objects also consume memory. Small exact tests use three arrays but never benchmark-scale clones. The library parallel sort may also require an auxiliary array. The maximum benchmark size is deliberately limited to 30 million, leaving room inside the heap for the JVM's managed objects and allocation behavior.

**Heap capacity is not total process RAM.** Native JVM memory, code, threads, libraries and the controller also count. These conservative settings leave headroom under even a decimal 1,000,000,000-byte budget, but cannot mathematically enforce total resident RAM on every JVM/OS. For strict grading, run inside an OS/container memory limit of 1,000,000,000 bytes and record peak memory for the whole process tree. On Linux with a preinstalled JDK 21 container image, an example is:

```text
docker run --rm --memory=1000000000 --memory-swap=1000000000 --cpus=4 -v "${PWD}:/work" -w /work eclipse-temurin:21-jdk java -Xmx64m -cp build Benchmark full results/limited.csv
```

That optional container command is not required for ordinary local testing and was not used for the supplied local measurements. On Windows use an externally configured Job Object/process-tree memory limit if strict enforcement is required. Do not describe `-Xmx` alone as proof of the RAM constraint.

A daemon watchdog is armed immediately before the sort and terminates the worker around 15 seconds (`SORT_CAP`); its deadline includes a small thread-launch overhead. Scheduling is best-effort, not a real-time guarantee. Completed runs are also checked against the measured duration. Setup over 15 seconds is recorded as `SETUP_CAP`. The parent has a separate 90-second dead-worker timeout covering startup, warm-up, setup, sort and validation. An error or cap stops remaining trials and larger sizes for that algorithm/pattern. Timeout rows deliberately have no fabricated measured sorting time. Close to the boundary, rerun with smaller size increments and three or more trials.

The full grid doubles n after one million until 16 million and then tests the configured ceiling at 30 million. Report the largest **tested** size that succeeds within 15 seconds, separately per pattern and algorithm. If 30 million completes sooner, the configured conservative size/memory ceiling was reached before the time limit; this does not prove the absolute maximum capacity under 1 GB. Do not extrapolate a larger valid capacity beyond the configured RAM budget. For a timeout interval, refine it with the custom size argument. Never present projected timing as a measurement.

## Files and development record

- `src/FastSort.java`: submission algorithm and explicit width variants.
- `src/SortSupport.java`: dispatch, generators and validation.
- `src/SortTests.java`: exact deterministic correctness suite.
- `src/Benchmark.java`: isolated benchmark workers, caps and CSV output.
- `src/Summarize.java`: median/min/max Markdown tables from a benchmark CSV.
- `results/`: measured observations and environment metadata.
- `RESULTS.md`: findings and limitations from this development run.

Process chosen before measurement: preserve a reproducible seeded workload and raw timing records; implement three candidate widths plus library baselines; use exact tests to establish signed ordering and buffer correctness; measure each width under the same limits; retain all results and distinguish timing caps from memory ceilings. The package was produced with generative-AI assistance from the assignment discussion. The student should review and understand the implementation, rerun the experiments, and describe any subsequent changes and observations in their own report. No performance claim is inferred solely from asymptotic complexity.

Library baseline reference: [Oracle Arrays API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html). The API specifies ascending numerical ordering; implementation details can vary by JDK, so the recorded runtime version matters.
