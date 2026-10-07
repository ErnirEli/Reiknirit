import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class largestSquare {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int intak = scanner.nextInt();
        scanner.nextLine();

        List<List<Integer>> fences = new ArrayList<>();

        for (int i = 0; i < intak; i++) {
            String input = scanner.nextLine();
            String[] parts = input.split("\\s+");
            List<Integer> fence = new ArrayList<>();
            for (String part: parts){
                fence.add(Integer.parseInt(part));
            }
            fences.add(fence);
        }
        int biggest = 0;
        int counter = -1;
        int ans = -1;

        for (List<Integer> pair: fences){
            counter ++;
            int a = pair.get(0);
            int b = pair.get(1);
            if (a * b > biggest) {
                if (a == b) {
                biggest = pair.get(0) * pair.get(1);
                ans = counter;
                }

            }

        }
        System.out.println(fences.get(ans).get(0)+ " " +fences.get(ans).get(1));
        scanner.close();

    }
}