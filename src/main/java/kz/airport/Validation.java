package kz.airport;

/** Объектілерге бос міндетті мәтіннің түсуіне жол бермейді. */
final class Validation {
    private Validation() {
    }

    static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " бос болмауы керек.");
        }
        return value.trim();
    }
}
