import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class lowSkillDecks {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int intak_1 = scanner.nextInt();
        scanner.nextLine();

        List<String> lowSkill = new ArrayList<>();

        for (int i = 0; i < intak_1; i++) {
            String card = scanner.nextLine();
            lowSkill.add(card);
        }
        
        int intak_2 = scanner.nextInt();
        scanner.nextLine();

        for (int i = 0; i < intak_2; i++) {
            boolean lowSkillBool = false;
            for (int j = 0; j < 6; j++) {
                String card = scanner.nextLine();
                if (lowSkill.contains(card)) {
                    lowSkillBool = true;
                }
            }if (lowSkillBool) {
                System.out.println("Hæfileikalaust Drasl");
            }
            else {
                System.out.println("Fínn Stokkur");
            }
        }
        scanner.close();

    }
}
