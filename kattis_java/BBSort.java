import java.util.Scanner;

public class BBSort {

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

        boolean sorted = true;

        // for (int i = 0; i < arr.length - 1; i++) {
        //     if (arr[i] > arr[i + 1]) {
        //         sorted = false;
        //     }
        // }
        


        for (int i = 0; (k + i - 1) < arr.length; i++) {
            boolean s = bsort(arr, i, (k + i - 1));
            sorted = s & sorted;
        }

        if (sorted) {
            return hours;
        }

        return 0 + bbSort(arr, k, hours + 1);

    }
    
    private static boolean bsort(int[] arr, int start, int end) {
        boolean sorted = true;
        for (int k = start; k < end; end--) {
            for (int i = start; i < end; i++) {
                if (arr[i] > arr[i + 1]) {
                    exch(arr, i, i + 1);
                    sorted = false;
                }
            }
        }
        return sorted;
    }

    private static void exch(int[] arr, int j, int k) {
        int temp = arr[j];
        arr[j] = arr[k];
        arr[k] = temp;
    }
}

