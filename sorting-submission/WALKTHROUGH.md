# Understanding the sorting submission

The project answers two questions: **does the sorter produce the right answer?**
and **how quickly does it do so on different inputs?** These are separate checks.
The sorter rearranges Java `long` values from smallest to largest. A `long` is a
signed 64-bit integer, from -2^63 to 2^63 - 1. Sorting modifies the supplied array;
it does not return a new array. In the benchmark, `n` always means element count.

## A useful reading order

1. `src/FastSort.java`: the algorithm you are measuring.
2. `src/SortSupport.java`: how inputs are generated and results checked.
3. `src/SortTests.java`: how small and medium cases are compared exactly.
4. `src/Benchmark.java`: how isolated, timed experiments run.
5. `src/Summarize.java`: how raw observations become a readable table.
6. `results/README.md`, then `RESULTS.md`: how to read the data and conclusions.

`README.md` is the run/reference manual. `run.ps1` automates the workflow in
PowerShell; `run.sh` does the same in a Unix shell. `build/` contains compiler
output (`.class` files), which Java executes. Those are binary files, not source
to read or comment; rebuild them from `src/`. The files under `results/` are
historical evidence, with their contents explained in `results/README.md`.

The overall flow is:

```text
run.ps1 or run.sh
  javac          -> compile src/*.java into build/*.class
  SortTests      -> verify outputs against Arrays.sort
  Benchmark      -> generate inputs, measure sorts, write results/*.csv
  Summarize      -> read CSV, print results/*-summary.md
```

## FastSort: why sorting by digits works

First, the method rejects invalid arguments. Arrays shorter than 2,048 elements
use `Arrays.sort`, avoiding the cost of radix buffers. Larger arrays are scanned:
an ascending array needs no work; a descending array can simply be reversed.
Equal neighbouring values are permitted in either order. If neither condition
holds, radix sorting begins.

LSD means *least significant digit*: process the lowest digit first. To see why,
use decimal numbers as a small illustration (the actual code uses binary digits):

```text
Input:                       21, 13, 12, 23
Group by ones, keeping ties:  21, 12, 13, 23
Group by tens, keeping ties:  12, 13, 21, 23
```

Keeping ties in their existing order is called **stability**. During the tens
pass, 12 stays before 13 because the ones pass already placed them correctly.
After each pass, the values are ordered by all digits processed so far. Once
every digit has been processed, the complete keys are ordered.

With 8 bits per digit there are 256 possible digit values, and a 64-bit number
needs eight passes. Width 11 uses six passes and width 16 uses four. The last
11-bit pass uses only nine bits. Wider digits mean fewer passes but larger bucket
tables and more scattered memory access, so they are not automatically faster.

### One pass, step by step

1. Zero the bucket array, then count how many values have each current digit.
2. Replace counts with starting positions. Counts `[2, 3, 1]` become `[0, 2, 5]`:
   bucket 0 occupies positions 0-1, bucket 1 positions 2-4, bucket 2 position 5.
3. If one bucket contains every element, skip the pass: this digit changes nothing.
4. Read the source from left to right. Write each value at its bucket's next free
   position, then increment that position. This is the stable scatter.
5. Swap source and destination references. The newly written array becomes the
   next pass's input. No elements are copied by this reference swap.

After skipped passes, the final source might be the temporary array. The final
`System.arraycopy` puts the answer back in the original array when needed.

### Negative numbers and the digit expression

The expression is:

```java
int digit = (int) ((value ^ Long.MIN_VALUE) >>> shift) & mask;
```

Java represents negative integers in two's complement. Their highest bit is 1,
so treating the raw bits as unsigned would put negatives after nonnegatives.
`Long.MIN_VALUE` has only that highest bit set. XOR (`^`) flips it, producing keys
whose unsigned order matches the original signed order:

| Original value | Key after flipping the sign bit, in hexadecimal |
|---|---|
| `Long.MIN_VALUE` | `0000000000000000` |
| `-1` | `7fffffffffffffff` |
| `0` | `8000000000000000` |
| `Long.MAX_VALUE` | `ffffffffffffffff` |

`>>> shift` moves the wanted digit to the low end, filling vacated bits with zero.
The cast to `int` keeps the low 32 bits; the digit needs at most 16 of them.
`& mask` retains only this pass's digit bits. For eight bits, `mask` is 255,
or binary `11111111`. Only the ordering key is transformed; the original values
are written to the destination.

### Time and memory

Each pass scans the array and its bucket table, giving
`O(ceil(64 / bits) * (n + 2^bits))` time. With a fixed supported width this is
`O(n)`. The general radix path needs a second `long[n]` and an `int[2^bits]`.
The ordered fast paths for arrays of at least 2,048 elements use constant extra
space. This is an array-mutating sort, but the general path is not constant-space.

## SortSupport: inputs and checks

`sort` translates a method name into a call. The three radix names select digit
widths; `jdk` uses `Arrays.sort`; `parallel` uses `Arrays.parallelSort`, which may
use several threads.

`input` generates four workloads. `random` draws full-width signed longs from a
seeded generator. `sorted` computes evenly spaced ascending values without first
sorting them. `reverse` reverses that sequence. `partial` performs about `n/100`
random pair swaps on it. That is a count of swaps, not a guarantee that exactly
1% of elements move. Identical size, pattern and seed mean identical input.

The ordered generator treats the bits of `-1L` as the unsigned number 2^64 - 1,
divides that span into gaps and starts at `Long.MIN_VALUE`. Multiplication and
addition intentionally wrap in Java's 64-bit arithmetic. For three elements,
the resulting sequence is `Long.MIN_VALUE`, `-1`, `Long.MAX_VALUE - 1`.
Rounding the gap down means the final element need not reach `Long.MAX_VALUE`.

`validate` checks every adjacent pair for ascending order. It also compares three
order-independent checksums with a fingerprint captured earlier: the sum of
values, the sum of mixed values and the XOR of mixed values. The `mix` function
scrambles bits to make changes less likely to cancel out. Overflow is intentional.
A **multiset** means values together with their occurrence counts, ignoring order.
Matching fingerprints are evidence of preserved contents, not an exact proof:
different multisets can have the same checksums.

## SortTests: exact comparisons

For each test input, `check` clones the array and library-sorts that clone to make
an expected answer. Each method receives a separate clone of the original input;
`Arrays.equals` then compares every element. The public default is checked too.
Each input therefore produces six comparisons: five named methods plus the default.

Tests cover empty and singleton arrays, signed extremes, the 2,048-element
boundary, all four patterns, duplicate-heavy data and each individual bit.
Single-bit inputs leave most digits constant, testing skipped passes and buffer
copying. Another 100 seeded random cases vary size and contents. Finally, null
input and an unsupported width must throw the expected exceptions.
The suite reports 4,452 array comparisons, with the two exception checks separate.

## Benchmark: controller, worker and clock

The controller reads optional arguments, records the environment and visits sizes
in ascending order. For each repetition it shuffles method/pattern pairs using a
fixed seed. Each pair starts a fresh JVM running `Benchmark worker ...`. This is
why the same class has two roles. A process is a separate running program; the
watchdog described below is a thread *inside* a worker process.

A worker does the following:

1. Runs 16 warm-up sorts so frequently used code can be compiled and optimized
   by the JVM. Warm-up validation checks ordering; its fingerprint is taken after
   sorting, so it does not independently verify preservation of warm-up contents.
2. Requests garbage collection, then measures input generation separately.
3. Takes the measured input's fingerprint before sorting.
4. Starts the deadline watchdog, then times one call to `SortSupport.sort`.
5. Signals completion, validates the result and prints a CSV row.

`System.nanoTime()` supplies elapsed-time readings. Subtract start from end;
divide by 1,000,000 (`1e6`) to obtain milliseconds. Sorting time includes dispatch,
ordered scans, scratch allocation, radix work, final copying and any collection
during the sort. It excludes startup, warm-up, generation, fingerprinting,
validation and output.

The watchdog waits up to approximately 15 seconds. A shared lock protects its
deadline state and the sorting thread's completion flag. `timedWait` releases the
lock while waiting; `notifyAll` wakes it when sorting finishes. A loop checks the
condition again after every wake. The watchdog is a daemon, so it cannot keep an
otherwise finished JVM alive. On expiry it prints `SORT_CAP` and halts the worker.
The controller survives because it is a separate process. Its 90-second fallback
also covers startup, warm-up and validation. Input generation has its own
15-second check, performed after generation returns.

The watchdog deadline starts slightly before the sort timer to include thread
startup. Scheduling can delay it; it is not an exact real-time deadline. A completed
sort's measured duration is checked too. Once a method/pattern reports a non-OK
row, the controller skips its remaining repetitions and larger sizes.

Memory flags limit the worker heap and some other JVM regions. They do not cap
total operating-system memory. The README explains those settings and the
original allocation failures; `RESULTS.md` explains the retest.

## Summarize: observations into a table

The summary reader skips the CSV header and splits each row into eight columns.
The `-1` argument to `split` retains empty fields in failure rows. Its key combines
method, pattern and size. One map stores successful times for each key; another
counts non-OK rows. `computeIfAbsent` creates a list only when the key is new;
`merge` inserts a failure count of one or adds one to the existing count.

Each time list is sorted. The first and last values are minimum and maximum.
For an odd number of observations, the median is the middle one; for an even
number, average the two middle ones. `(count - 1) / 2` and `count / 2` select both
cases because integer division rounds down for these nonnegative indexes.
Only successful trials contribute times. Failed counts do not include skipped
jobs because those never produced rows. `Locale.ROOT` gives consistent decimal
points, and `%.3f` prints three digits after the point.

## Java notation used in these files

| Notation | Meaning here |
|---|---|
| `static` | A field or method belongs to the class; no object is needed to use it. |
| `final` | A variable cannot be reassigned; on a class it prevents subclassing. Array contents can still change. |
| `long[]` / `int[]` | An array of long integers / ordinary 32-bit integers. Indexes start at zero. |
| `for (long value : source)` | Visit each element in array order. |
| `++` | Increment by one; postfix uses the previous value in the surrounding expression. |
| `^`, `&`, `>>>`, `<<` | Bitwise XOR, AND, unsigned right shift and left shift. |
| `&&`, `||`, `!` | Boolean AND, OR and NOT; AND/OR can skip evaluating the right operand. |
| `ascending &= condition` | Update the boolean to `ascending & condition`: once false, it stays false. |
| `constantDigit |= condition` | Boolean OR assignment: once true, it stays true. |
| `condition ? yes : no` | Choose one of two values based on a boolean condition. |
| `1L`, `1e6`, `30_000_000` | A long literal, one million as a floating-point number, and a readable integer literal. |
| `List<T>`, `Map<K,V>`, `Set<T>` | A growable sequence, key/value lookup and collection of unique values. |
| `->` | A lambda: code passed to another method, such as the watchdog thread body. |
| `Integer::parseInt` | A method reference: use this function to convert each text size to an integer. |
| `try (...)` | Automatically close a resource when the block ends. |
| `finally` | Cleanup that runs when leaving the block, including after an exception. |
| `throw` / `catch` | Report an exceptional condition / handle an expected exception. |
| `synchronized (lock)` | Only one thread at a time executes code guarded by this lock; updates become visible to other lock users. |

## Try the project in small steps

From this folder, compile and run the correctness suite first:

```text
javac -Xlint:all -d build src/FastSort.java src/SortSupport.java src/SortTests.java src/Benchmark.java src/Summarize.java
java -Xmx640m -cp build SortTests
```

Then try one repetition at one small size, using a separate output filename.
This still runs all four patterns for each named method and includes worker warm-up:

```text
java -Xmx64m -cp build Benchmark quick results/learning.csv 1 radix8,jdk 10000
java -cp build Summarize results/learning.csv
```

Follow one output row through `results/README.md`, then find the code that writes
each column. To study the algorithm in a debugger, use a mixed array of at least
2,048 values so it reaches the radix path, and inspect bucket counts, prefix sums
and the source/destination reference swap after the first pass.
