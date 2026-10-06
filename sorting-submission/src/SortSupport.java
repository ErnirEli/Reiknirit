import java.util.Arrays;
import java.util.SplittableRandom;

/** Shared method names, reproducible input generation and correctness checks. */
final class SortSupport {
    static final String[] METHODS = {"radix8", "radix11", "radix16", "jdk", "parallel"};
    static final String[] PATTERNS = {"sorted", "partial", "random", "reverse"};

    /** Dispatches a benchmark name to an implementation; each modifies values. */
    static void sort(String method, long[] values) {
        switch (method) {
            case "radix8":
                FastSort.radixSort(values, 8);
                break;
            case "radix11":
                FastSort.radixSort(values, 11);
                break;
            case "radix16":
                FastSort.radixSort(values, 16);
                break;
            case "jdk":
                Arrays.sort(values);
                break;
            case "parallel":
                Arrays.parallelSort(values);
                break;
            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /** The same size, pattern and seed produce the same array for every method. */
    static long[] input(int size, String pattern, long seed) {
        long[] values = new long[size];
        SplittableRandom random = new SplittableRandom(seed);
        if (pattern.equals("random")) {
            for (int i = 0; i < size; i++) {
                values[i] = random.nextLong(); // Full signed 64-bit range.
            }
        } else {
            if (!Arrays.asList(PATTERNS).contains(pattern)) {
                throw new IllegalArgumentException(pattern);
            }
            // As unsigned bits, -1L represents 2^64 - 1, the full range width.
            // Divide it into size-1 gaps, then start at the smallest signed long.
            // Java's wrapping long arithmetic is intentional here: the resulting
            // bit patterns describe ascending, evenly spaced signed values.
            // With 0 or 1 element there is no gap, so avoid dividing by zero.
            long step = size < 2 ? 0 : Long.divideUnsigned(-1L, size - 1);
            for (int i = 0; i < size; i++) {
                values[i] = Long.MIN_VALUE + step * i;
            }
            if (pattern.equals("reverse")) {
                for (int i = 0; i < size / 2; i++) {
                    swap(values, i, size - i - 1);
                }
            } else if (pattern.equals("partial") && size > 1) {
                // Perform about size/100 random pair swaps (at least one).
                // This does NOT mean exactly 1% of positions end up changed:
                // indexes can repeat and a swap can select the same index twice.
                for (int i = 0; i < Math.max(1, size / 100); i++) {
                    swap(values, random.nextInt(size), random.nextInt(size));
                }
            }
        }
        return values;
    }

    /** Exchanges two positions without allocating another array. */
    static void swap(long[] values, int first, int second) {
        long temporary = values[first];
        values[first] = values[second];
        values[second] = temporary;
    }

    /** Scrambles bits for a checksum; this is not encryption or a sort key. */
    static long mix(long value) {
        // Unsigned shifts and odd multipliers spread changes across the bits.
        // Overflow intentionally wraps modulo 2^64, as in ordinary Java longs.
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    /**
     * Three order-independent checksums: raw sum, mixed sum and mixed XOR.
     * Reordering an array preserves them. Collisions are possible, so these
     * detect likely content corruption but cannot prove exact equality of multisets.
     */
    static long[] fingerprint(long[] values) {
        long sum = 0;
        long mixedSum = 0;
        long mixedXor = 0;
        for (long value : values) {
            sum += value;
            long mixed = mix(value);
            mixedSum += mixed;
            mixedXor ^= mixed;
        }
        return new long[]{sum, mixedSum, mixedXor};
    }

    /** Checks ascending order and compares with a fingerprint taken before sorting. */
    static void validate(long[] values, long[] before) {
        for (int i = 1; i < values.length; i++) {
            if (values[i - 1] > values[i]) {
                throw new AssertionError("Out of order at " + i);
            }
        }
        if (!Arrays.equals(before, fingerprint(values))) {
            throw new AssertionError("Multiset fingerprint changed");
        }
    }
}
