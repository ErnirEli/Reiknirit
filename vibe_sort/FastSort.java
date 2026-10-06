import java.util.Arrays;
import java.util.Random;

public class FastSort {

    // We sort 16 bits at a time.
    // A long has 64 bits, so this gives us 4 passes.
    private static final int BITS = 16;
    private static final int RADIX = 1 << BITS;   // 65536
    private static final int MASK = RADIX - 1;

    /**
     * Sorts the array in ascending order using LSD radix sort.
     *
     * @param a the array to sort
     */
    public static void radixSort(long[] a) {
        long[] aux = new long[a.length];
        int[] count = new int[RADIX];

        long[] source = a;
        long[] destination = aux;

        // 64 bits / 16 bits per pass = 4 passes
        for (int shift = 0; shift < 64; shift += BITS) {
            Arrays.fill(count, 0);

            // Count how many values belong to each 16-bit group.
            for (long value : source) {
                // Flip the sign bit so signed long values sort correctly.
                long key = value ^ Long.MIN_VALUE;
                int digit = (int) ((key >>> shift) & MASK);
                count[digit]++;
            }

            // Change counts into starting positions.
            int position = 0;
            for (int i = 0; i < RADIX; i++) {
                int amount = count[i];
                count[i] = position;
                position += amount;
            }

            // Put values into the correct positions in the other array.
            for (long value : source) {
                long key = value ^ Long.MIN_VALUE;
                int digit = (int) ((key >>> shift) & MASK);
                destination[count[digit]++] = value;
            }

            // Swap arrays for the next pass.
            long[] temp = source;
            source = destination;
            destination = temp;
        }

        // There are 4 passes, so the final result ends up back in a[].
    }

    /** Returns true if the array is sorted in ascending order. */
    public static boolean isSorted(long[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    /** Creates a uniformly random array of long values. */
    public static long[] randomArray(int n, long seed) {
        Random random = new Random(seed);
        long[] a = new long[n];

        for (int i = 0; i < n; i++) {
            a[i] = random.nextLong();
        }

        return a;
    }

    /** Creates an already sorted array. */
    public static long[] sortedArray(int n) {
        long[] a = new long[n];

        for (int i = 0; i < n; i++) {
            a[i] = i;
        }

        return a;
    }

    /** Creates an array in reverse order. */
    public static long[] reverseArray(int n) {
        long[] a = new long[n];

        for (int i = 0; i < n; i++) {
            a[i] = n - i;
        }

        return a;
    }

    /** Creates a mostly sorted array with a small number of random swaps. */
    public static long[] partiallySortedArray(int n) {
        long[] a = sortedArray(n);
        Random random = new Random(12345);

        int swaps = Math.max(1, n / 100); // swap about 1% of positions

        for (int i = 0; i < swaps; i++) {
            int first = random.nextInt(n);
            int second = random.nextInt(n);

            long temp = a[first];
            a[first] = a[second];
            a[second] = temp;
        }

        return a;
    }

    /**
     * Times only the call to radixSort().
     * Array creation is done before this method is called.
     */
    public static double benchmark(long[] a) {
        long start = System.nanoTime();
        radixSort(a);
        long end = System.nanoTime();

        if (!isSorted(a)) {
            throw new RuntimeException("Sort failed: array is not sorted.");
        }

        return (end - start) / 1_000_000_000.0;
    }

    public static void main(String[] args) {
        // Small correctness test including negative numbers and duplicates.
        long[] correctnessTest = {
            5, -10, 0, Long.MAX_VALUE, Long.MIN_VALUE, 5, -1, 100
        };

        long[] expected = correctnessTest.clone();
        Arrays.sort(expected);

        radixSort(correctnessTest);

        if (!Arrays.equals(correctnessTest, expected)) {
            throw new RuntimeException("Correctness test failed.");
        }

        System.out.println("Correctness test passed.\n");

        // Warm up the JVM before measuring larger tests.
        for (int i = 0; i < 3; i++) {
            long[] warmup = randomArray(100_000, i);
            radixSort(warmup);
        }

        // Scaling test on random input.
        int[] sizes = {
            100_000,
            500_000,
            1_000_000,
            2_000_000,
            5_000_000,
            10_000_000,
            20_000_000
        };

        System.out.println("Random input scaling test");
        System.out.println("n,time_seconds");

        for (int n : sizes) {
            long[] a = randomArray(n, 12345);
            double seconds = benchmark(a);

            System.out.printf("%d,%.6f%n", n, seconds);

            // Stop before going far beyond the assignment's 15 second limit.
            if (seconds >= 12.0) {
                break;
            }
        }

        // Compare the input types mentioned in the assignment.
        int testSize = 5_000_000;

        System.out.println("\nInput type test with n = " + testSize);
        System.out.printf("Sorted:           %.6f seconds%n",
                benchmark(sortedArray(testSize)));
        System.out.printf("Partially sorted: %.6f seconds%n",
                benchmark(partiallySortedArray(testSize)));
        System.out.printf("Random:           %.6f seconds%n",
                benchmark(randomArray(testSize, 12345)));
        System.out.printf("Reverse:          %.6f seconds%n",
                benchmark(reverseArray(testSize)));
    }
}
