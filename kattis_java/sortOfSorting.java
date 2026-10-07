public class sortOfSorting {

    public static void main(String[] args) {

        Kattio io = new Kattio(System.in, System.out);

        int num = io.getInt();

        while (num != 0) {

            String[] names = new String[num];

            for (int i = 0; i < num; i++) {
                names[i] = io.getWord();
            }

            sort(names);

            for (int i = 0; i < num; i++) {
                io.println(names[i]);
            }

            num = io.getInt();

        }
        io.close();
    }

    private static void sort(String[] arr) {

        int end = arr.length - 1;
        
        for (int i = 0; i < end; end--) {
            for (int j = 0; j < end; j++) {
                if (arr[j].substring(0, 2).compareTo(arr[j + 1].substring(0, 2)) > 0)  {
                    exch(arr, j, j+1);
                }
            }

        }
    }

    private static void exch(String[] arr, int i, int j) {
        String temp = arr[j];
        arr[j] = arr[i];
        arr[i] = temp;
    }
}