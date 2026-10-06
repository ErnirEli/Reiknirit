import java.util.Arrays;
import java.util.Objects;

/** Sorts signed 64-bit integers in ascending order, modifying the supplied array. */
public final class FastSort {
    // Static functions only: callers do not need a FastSort object.
    private FastSort() {}

    /** Uses the 8-bit variant selected by the recorded benchmark results. */
    public static void sort(long[] values) {
        radixSort(values, 8);
    }

    /**
     * Least-significant-digit (LSD) radix sort: process low bits before high bits.
     * A digit means a group of bits. Supported widths are 8, 11 and 16 bits.
     * Rejects null arrays and unsupported widths, including for empty arrays.
     */
    public static void radixSort(long[] values, int bits) {
        Objects.requireNonNull(values, "array");
        if (bits != 8 && bits != 11 && bits != 16) {
            throw new IllegalArgumentException("bits must be 8, 11 or 16");
        }
        // Small inputs use the library sort to avoid radix-buffer overhead.
        if (values.length < 2048) {
            Arrays.sort(values);
            return;
        }

        // Equal neighbours are allowed in either direction. Stop as soon as
        // neither direction is possible: a general sort is then necessary.
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < values.length && (ascending || descending); i++) {
            ascending &= values[i - 1] <= values[i];
            descending &= values[i - 1] >= values[i];
        }
        if (ascending) {
            return;
        }
        if (descending) {
            for (int left = 0, right = values.length - 1; left < right; left++, right--) {
                long temporary = values[left];
                values[left] = values[right];
                values[right] = temporary;
            }
            return;
        }

        // Each pass reads one array and writes the other. Swap references to
        // reuse these two arrays instead of allocating a new one for every digit.
        long[] source = values;
        long[] destination = new long[values.length];
        int[] bucketPositions = new int[1 << bits]; // 2^bits possible digits.
        for (int shift = 0; shift < 64; shift += bits) {
            // The 11-bit variant ends with 9 bits: 64 - 5 * 11 = 9.
            int radix = 1 << Math.min(bits, 64 - shift);
            int mask = radix - 1; // Low bits are all 1, selecting this digit.
            Arrays.fill(bucketPositions, 0);

            // XOR flips the sign bit so negative values precede nonnegative
            // values in unsigned key order. >>> shifts in zeros; & keeps a digit.
            // This changes the key only: we store the original long values.
            for (long value : source) {
                int digit = (int) ((value ^ Long.MIN_VALUE) >>> shift) & mask;
                bucketPositions[digit]++;
            }

            // Exclusive prefix sums turn frequencies into bucket start indexes.
            // Example: counts [2, 3, 1] become start positions [0, 2, 5].
            int nextStart = 0;
            boolean constantDigit = false;
            for (int digit = 0; digit < radix; digit++) {
                int frequency = bucketPositions[digit];
                constantDigit |= frequency == values.length;
                bucketPositions[digit] = nextStart;
                nextStart += frequency;
            }
            if (constantDigit) {
                // No ordering can change for this digit. No writes means no swap.
                continue;
            }

            // Reading left to right and advancing bucket positions is stable:
            // equal digits retain their previous relative order. This preserves
            // the order established by all previously processed lower digits.
            for (long value : source) {
                int digit = (int) ((value ^ Long.MIN_VALUE) >>> shift) & mask;
                destination[bucketPositions[digit]++] = value;
            }
            long[] temporary = source;
            source = destination;
            destination = temporary;
        }
        // Skipping digits may leave an odd number of actual passes. The caller
        // needs its original array object to contain the final sorted values.
        if (source != values) {
            System.arraycopy(source, 0, values, 0, values.length);
        }
    }
}
