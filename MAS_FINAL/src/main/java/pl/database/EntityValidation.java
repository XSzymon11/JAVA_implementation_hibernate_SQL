package pl.database;

import java.math.BigDecimal;
import java.time.LocalDate;

final class EntityValidation {
    private EntityValidation() {}

    static void requireNonBlank(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireNonNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireMaxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requirePositive(double value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    static void requireDateRange(LocalDate from, LocalDate to, String message) {
        requireNonNull(from, message);
        requireNonNull(to, message);
        if (to.isBefore(from)) {
            throw new IllegalArgumentException(message);
        }
    }
}
