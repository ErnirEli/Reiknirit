# Development results — 7 September 2026

## Outcome

Reading guide: `30m` means 30 million array elements; `ms` means milliseconds
(1,000 ms = 1 second). Each median is the middle of three sorted trial times.
`radix8`, `radix11` and `radix16` differ in bits handled per pass; `jdk` is the
sequential Java library baseline and `parallel` may use multiple threads.
The raw observations and their columns are explained in [results/README.md](results/README.md).
These are historical measurements, not new measurements of the readability edits.

The final default is **8-bit adaptive radix sort**. On the tested machine, it sorted **30,000,000 random signed longs in a median 1.629 seconds**, including auxiliary allocation. All 120 observations in the final large-array retest completed with status `OK`, including validation, with every sort below 15 seconds. This establishes the largest tested successful size, not a universal maximum or a guarantee for every input/machine.

At that size the two radix arrays have a combined payload of 480,000,000 bytes. Workers used a 640 MiB heap with the generation layout described below. **Total resident process-tree RAM was not measured or externally capped in these local runs.** Strict 1 GB compliance should therefore be verified using the OS/container procedure in the README; heap limits alone do not establish it.

## Final retest: median milliseconds, three trials per cell

| Method | Sorted, 30m | Partial, 30m | Random, 30m | Reverse, 30m |
|---|---:|---:|---:|---:|
| Radix 8 (selected default) | 93.450 | 1532.494 | 1628.772 | 125.471 |
| Radix 11 | 66.649 | 2485.009 | 1997.234 | 129.961 |
| Radix 16 | 90.669 | 4241.711 | 3080.208 | 158.928 |
| Arrays.sort | 37.499 | 1583.321 | 5306.737 | 126.440 |
| Arrays.parallelSort | 34.291 | 698.635 | 2229.405 | 104.357 |

The 8-bit random-input times ranged from 1594.550 to 2238.008 ms. Its random median was about 3.26 times faster than sequential `Arrays.sort` in this retest. The parallel baseline was substantially faster on the partially sorted workload. Neither the custom algorithm nor one radix width wins everywhere. Ordered fast paths contain the same code in all three radix variants; differences between their ordered-input measurements reflect run conditions/JIT variation rather than radix work.

At 16 million random elements, final median times were 779.224 ms (8-bit), 1764.120 ms (11-bit), 1618.724 ms (16-bit), 3027.231 ms (sequential library) and 1060.997 ms (parallel library). These data support selecting 8 bits for this submission's large-input target despite its higher pass count. They do not establish a crossover threshold or justify an input-size-dependent width selector.

The largest final sorting observation was 9836.318 ms for 16-bit radix on 30 million partially sorted values. The configured 30-million size ceiling was reached before the 15-second deadline. No 15-second maximum capacity is claimed or extrapolated. Timeout handling exists in the runner, but no final run reached that timeout.

## What was run and changed

1. Implemented 8-, 11- and 16-bit stable LSD radix variants, initially choosing 11 bits provisionally, with ascending/reverse fast paths and a small-input library fallback. Added sequential and parallel library baselines.
2. Exact tests passed 4,452 comparisons with a separately sorted reference, including the public default, signed extremes, duplicate-heavy data, every bit position, threshold boundaries and buffer-parity cases.
3. Ran the seven-size full grid with three trials and all four patterns. `results/full.csv` contains 408 observations: 402 successes and six allocation failures. The remaining 12 planned observations were skipped after failures. This initial run used Serial GC's default `NewRatio=2`.
4. Diagnosed the six failures: the radix auxiliary allocation at 30 million elements threw `OutOfMemoryError: Java heap space`. With a 640 MiB heap and the default generation split, the approximately 426.7 MiB old generation could not hold both approximately 228.9 MiB arrays, while each array exceeded the approximately 213.3 MiB maximum young generation. Payload below total heap capacity was not sufficient.
5. Set `-XX:NewRatio=8` to provide a larger old generation without raising the heap limit. Retested **all five methods**, all four patterns, 16 million and 30 million elements, three trials each. `results/tuned.csv` contains these 120 successful observations. The source runner now uses this setting. Its sidecar records the changed configuration.
6. Selected 8 bits as the public default based on the large-input measurements and reran the exact correctness suite. The radix8 benchmark path invokes the same implementation and width as the final public default.

The initial full grid is retained as development evidence, not silently replaced. Do not merge observations from the two collector configurations into a single median. The initial run provides broad scaling measurements, while the final run verifies both the memory fix and large-size comparisons under uniform final settings. To reproduce the final retest:

```text
java -Xmx64m -cp build Benchmark full results/retest.csv 3 radix8,radix11,radix16,jdk,parallel 16000000,30000000
java -cp build Summarize results/retest.csv
```

Use the ordinary `full` command from the README to repeat all seven sizes with the final settings.

## Environment and limits of interpretation

Windows amd64, Intel Core i5-8365U CPU @ 1.60 GHz, Temurin OpenJDK 21.0.12+8-LTS. Four active processors were exposed to each worker. Full runtime identifiers and commands are in each CSV's `.environment.txt` sidecar. Measurements ran on a shared desktop, without controlled CPU frequency, power mode or background load. There was substantial variability, especially during portions of the first sweep and the start of the retest. Three samples are a useful demonstration, not a statistical performance study; reported ratios are descriptive observations, not confidence-bounded conclusions.

The supplied Markdown summaries include min/max and failed counts for every case. CSV files retain individual seeds, setup times, sorting times and statuses. Exact test correctness and large-input probabilistic permutation validation are distinct, as explained in the README. The source and results were created with AI assistance; the memory-layout correction and empirical change from 11 to 8 bits are part of the documented development process.
