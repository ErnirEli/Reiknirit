import java.util.Arrays;

public class BBSort{

    public static void main(String[] args) {

        Kattio io = new Kattio(System.in, System.out);

        int n = io.getInt();
        int k = io.getInt();

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = io.getInt();
        
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        io.println(Arrays.toString(sorted));

        int highest = 0;

        for (int i = 0; i < n; i++) {
            int num = arr[i];
            int index = java.util.Arrays.binarySearch(sorted, num);
            if (index > highest) {
                highest = index;
            }
        }
        int x = (int) Math.ceil(highest/k);
        io.println(x);


        //int hours = bbSort(arr, k, 0);

        io.close();
        
    }


}