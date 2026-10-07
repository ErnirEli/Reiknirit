import java.util.Arrays;

public class planetaris {

    public static void main(String[] args) {

        Kattio io = new Kattio(System.in, System.out);

        int n = io.getInt();
        int k = io.getInt();

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = io.getInt();

        Arrays.sort(arr);

        int counter = 0;

        for (int i = 0; i < n; i++) {
            k -= arr[i] + 1;
            if (k < 0) {
                break;
            }
            counter += 1;
        }

        io.println(counter);

        io.close();

    }
    
}
