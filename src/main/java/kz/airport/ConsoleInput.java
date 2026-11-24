package kz.airport;

import java.util.Scanner;

/** Консольден мәтін мен санды қайта сұрап оқиды; EOF шақырушыға беріледі. */
public final class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = java.util.Objects.requireNonNull(scanner);
    }

    public String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Бос қалдырмай, мән енгізіңіз.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Таңдау тізімі бос немесе шектер қате.");
        }
        while (true) {
            String value = readText(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Қате енгізілсе, қайта сұраймыз.
            }
            System.out.println(
                    min + " мен " + max + " аралығындағы бүтін сан енгізіңіз."
            );
        }
    }
}
