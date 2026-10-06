import java.util.Arrays;
import java.util.SplittableRandom;

/** Standalone deterministic tests; run with java -cp build SortTests. */
public final class SortTests {
    private static int checks;

    /** Compares every output element with an independently library-sorted copy. */
    private static void check(long[] input) {
        long[] expected = input.clone();
        Arrays.sort(expected);
        for (String method : SortSupport.METHODS) {
            // A fresh clone gives every method the same unsorted input.
            long[] actual = input.clone();
            SortSupport.sort(method, actual);
            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError(method + ": n=" + input.length);
            }
            checks++;
        }
        // Also exercise the public default, in case its selected width changes.
        long[] actual = input.clone();
        FastSort.sort(actual);
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Default sorter");
        }
        checks++;
    }

    public static void main(String[] args) {
        check(new long[0]);
        check(new long[]{Long.MIN_VALUE});
        check(new long[]{Long.MAX_VALUE, 0, -1, Long.MIN_VALUE, 1, Long.MIN_VALUE, Long.MAX_VALUE});
        SplittableRandom random = new SplittableRandom(20260907); // Repeatable tests.

        // Include sizes immediately below, at and above the 2048-element fallback.
        for (int size : new int[]{2, 7, 31, 2047, 2048, 2049, 8193, 65537, 200000}) {
            for (String pattern : SortSupport.PATTERNS) {
                check(SortSupport.input(size, pattern, size));
            }
            long[] values = new long[size];
            check(values); // Newly allocated long arrays contain only zeroes.
            for (int i = 0; i < size; i++) {
                values[i] = random.nextInt(17) - 8; // Many duplicates, from -8 to 8.
            }
            check(values);
            for (int i = 0; i < size; i++) {
                values[i] = i % 2 == 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            check(values);

            // Vary only one bit at a time. This tests every digit (including the
            // sign bit and the 11-bit variant's short final digit). Most digits
            // are constant, exercising skipped passes and the final buffer copy.
            for (int bit = 0; bit < 64; bit++) {
                for (int i = 0; i < size; i++) {
                    values[i] = random.nextBoolean() ? 0 : 1L << bit;
                }
                check(values);
            }
        }
        for (int trial = 0; trial < 100; trial++) {
            check(SortSupport.input(random.nextInt(10000), "random", random.nextLong()));
        }

        // These calls must throw. If they return normally, the assertion fails.
        // Explicit AssertionError checks run even without Java's -ea flag.
        try {
            FastSort.sort(null);
            throw new AssertionError("null accepted");
        } catch (NullPointerException expected) {
            // Expected contract: null is not a valid input array.
        }
        try {
            FastSort.radixSort(new long[0], 12);
            throw new AssertionError("invalid width accepted");
        } catch (IllegalArgumentException expected) {
            // Expected contract: only widths 8, 11 and 16 are supported.
        }
        System.out.println("PASS: " + checks + " exact comparisons with Arrays.sort; null/width contracts checked.");
    }
}
