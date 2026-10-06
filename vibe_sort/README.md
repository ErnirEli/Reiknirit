# Fast Sorting of 64-bit Integers

## What this project does

This is a small Java program for the sorting assignment.

It sorts arrays of Java `long` values. A `long` is a signed 64-bit integer.

The program contains:

- one sorting algorithm: LSD radix sort
- a correctness test
- timing tests for increasing array sizes
- tests for sorted, partially sorted, random, and reverse input

The code is intentionally kept simple and in one Java file so it is easier to understand and explain.

---

## Why radix sort?

A normal comparison-based sorting algorithm repeatedly asks questions such as:

`Is value A smaller than value B?`

Algorithms such as merge sort generally need about `n log n` work for large inputs.

Here every value has a fixed size: exactly 64 bits.

Radix sort uses those bits directly instead of comparing pairs of numbers. In this implementation we look at 16 bits at a time.

Since:

`64 / 16 = 4`

we make exactly four passes over the data.

The general idea is similar to sorting decimal numbers by:

1. ones digit
2. tens digit
3. hundreds digit
4. and so on

The important difference is that this program works with binary digits instead of decimal digits.

---

## The main constants

```java
private static final int BITS = 16;
private static final int RADIX = 1 << BITS;
private static final int MASK = RADIX - 1;
```

`BITS = 16` means that one sorting pass looks at 16 bits of each number.

`RADIX = 65536` because 16 bits can represent 65,536 different values.

`MASK` is used to extract only the 16 bits that we want to inspect during a pass.

---

## How `radixSort` works

The method is:

```java
public static void radixSort(long[] a)
```

It creates two helper structures:

```java
long[] aux = new long[a.length];
int[] count = new int[RADIX];
```

`aux` is a temporary array where values are placed during each pass.

`count` stores how many numbers belong to each 16-bit group.

### Step 1: Count the groups

For every value we take the current 16-bit section and increase its counter.

```java
int digit = (int) ((key >>> shift) & MASK);
count[digit]++;
```

### Step 2: Calculate starting positions

The raw counts are converted into positions telling us where each group should begin in the destination array.

### Step 3: Move the values

Each value is copied into the correct place in the destination array.

### Step 4: Swap arrays

The source and destination arrays change roles and the next 16-bit section is processed.

This happens four times.

---

## Why do we use `value ^ Long.MIN_VALUE`?

Java `long` values are signed.

That means values such as `-10` must come before values such as `5`.

The binary representation makes this slightly awkward for radix sort because negative values have their highest bit set.

This line flips that highest bit:

```java
long key = value ^ Long.MIN_VALUE;
```

We only use the changed value as a sorting key. The original number is still copied into the output array.

Flipping the sign bit makes the binary order line up with normal Java signed-number order.

---

## Correctness testing

The program first tests a small array containing:

- positive values
- negative values
- zero
- duplicates
- `Long.MIN_VALUE`
- `Long.MAX_VALUE`

It sorts a copy using Java's built-in `Arrays.sort` and compares the two results.

If the results differ, the program stops with an error.

---

## Performance testing

The assignment says setup time should not be included in the sorting time.

Therefore the array is created before timing starts.

The timing itself is:

```java
long start = System.nanoTime();
radixSort(a);
long end = System.nanoTime();
```

This means the timer starts immediately before the sorting method is called and stops immediately after it returns.

Note: this implementation creates its temporary `aux` array inside `radixSort`, so that allocation IS included in the measured sorting time. This is the conservative and easy-to-explain choice.

The program also runs a few smaller warm-up sorts before benchmarking. Java uses a JIT compiler, so the first execution can be slower than later executions.

---

## Test cases

The program tests the four input styles mentioned in the assignment:

- sorted
- partially sorted
- uniformly random
- reverse order

It also measures random arrays of increasing sizes so you can show how runtime scales as `n` grows.

---

## Memory use

The two large arrays are:

- the input array: about `8 * n` bytes
- the temporary array: about `8 * n` bytes

So the main memory requirement is approximately:

`16 * n bytes`

There is also a counting array of 65,536 integers, which needs about 256 KB.

For example:

- 10,000,000 values -> roughly 160 MB for the two long arrays
- 20,000,000 values -> roughly 320 MB
- 50,000,000 values -> roughly 800 MB

Java itself also needs memory, so do not try to use the whole 1 GB limit for arrays.

---

## How to compile and run

Open a terminal in this folder.

Compile:

```bash
javac FastSort.java
```

Run:

```bash
java FastSort
```

If you want to put a clear Java heap limit on the program, for example 768 MB:

```bash
java -Xmx768m FastSort
```

This is useful because the assignment has a 1 GB memory limit.

---

## What the output looks like

You should get output similar to:

```text
Correctness test passed.

Random input scaling test
n,time_seconds
100000,...
500000,...
1000000,...
...

Input type test with n = 5000000
Sorted:           ... seconds
Partially sorted: ... seconds
Random:           ... seconds
Reverse:          ... seconds
```

Your exact times will be different because runtime depends on the computer, Java version, CPU, available memory, and other programs running at the same time.

---

## What you should understand before submitting

You do not need to memorize every bit operation.

The important story is:

1. A Java `long` has 64 bits.
2. We split those 64 bits into four 16-bit pieces.
3. We sort by one piece at a time, starting with the lowest bits.
4. Counting tells us where each group belongs.
5. After four passes, the whole number is sorted.
6. We flip the sign bit while calculating the key so negative numbers sort correctly.
7. We test correctness and then measure runtime for different inputs and sizes.

That is the core idea of the program.

---

## AI documentation

Generative AI was used through the ChatGPT web application to discuss the algorithm choice, write an initial Java implementation, simplify the code, and explain the benchmarking and memory considerations.

The final program should still be read, tested, and understood before submission. The assignment specifically values being able to explain the process and the code, not only producing a fast result.

---

## If Java says "Main method not found"

This version of `FastSort.java` contains the required method:

```java
public static void main(String[] args)
```

If you still get that error, you are probably running an old compiled `FastSort.class` or a different `FastSort.java` file.

From the folder containing this file, run:

```powershell
Remove-Item FastSort.class -ErrorAction SilentlyContinue
javac FastSort.java
java FastSort
```

You can also check which folder you are in with:

```powershell
Get-Location
```
