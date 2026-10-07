import java.util.Scanner;

public class creditCard {
    public static void main(String[] args) {
        int[] ps = {500, 1000, 2000, 5250, 11000, 24000};
        int[] isk = {500, 1000, 2000, 5000, 10000, 20000};

        Scanner scanner = new Scanner(System.in);

        int intak = scanner.nextInt();

        for (int i = 0; i < 6; i++) {
            if (ps[i] >= intak) {
                System.out.println(isk[i]);
                break;
            }
        }
        scanner.close();

        
    }
    
}
