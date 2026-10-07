public class BBSort_fail {

    public static void main(String[] args) {

        Kattio io = new Kattio(System.in, System.out);

        int n = io.getInt();
        int k = io.getInt();

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = io.getInt();

        int hours = bbSort(arr, k, 0);

        io.println(hours);

        io.close();
        
    }

    private static int bbSort(int[] arr, int k, int hours) {

        int[] before = arr.clone();
        boolean sorted = true;

        for (int i = 0; (k + i - 1) < arr.length; i++) {
            java.util.Arrays.sort(arr, i, (k + i));
        }

        for (int i = 0; i <= arr.length - 1; i++) {
            if (arr[i] != before[i]) {
                sorted = false;
            }
        }
        if (sorted) {
            return hours;
        }

        return bbSort(arr, k, hours + 1);

    }
    
    // private static boolean bsort(int[] arr, int start, int end) {
    //     boolean sorted = true;
    //     for (int k = start; k < end; end--) {
    //         for (int i = start; i < end; i++) {
    //             if (arr[i] > arr[i + 1]) {
    //                 exch(arr, i, i + 1);
    //                 sorted = false;
    //             }
    //         }
    //     }
    //     return sorted;
    // }

    // private static void exch(int[] arr, int j, int k) {
    //     int temp = arr[j];
    //     arr[j] = arr[k];
    //     arr[k] = temp;
    // }

}
