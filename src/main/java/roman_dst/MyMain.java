package roman_dst;

import java.util.Arrays;
import java.util.Scanner;

public class MyMain {
    public static void main(String[] args) {
        System.out.println("HELLo World!");
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\s+", 2);
            String command = parts[0];
            String[] params;

            if (command.equals("echo") && parts.length > 1) {
                params = new String[]{parts[1]};
            } else if (parts.length > 1) {
                params = parts[1].split("\\s+");
            } else {
                params = new String[]{};
            }

            System.out.println("Entered command = \"" + command + "\", parameters = " + Arrays.toString(params));

            if (command.equals("exit")) break;
        }
        scanner.close();
    }
}