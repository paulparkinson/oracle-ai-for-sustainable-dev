package com.oracle.demo.interactiveai;

import java.util.Set;

public final class InputValidation {
    private static final Set<String> ACTION_TYPES = Set.of("REVIEW", "CONTACT_OWNER", "FREEZE_CHANGES");

    private InputValidation() {
    }

    public static void minimumRisk(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException("minimumRisk must be between 0 and 100");
        }
    }

    public static void maximumRows(int value) {
        if (value < 1 || value > 50) {
            throw new IllegalArgumentException("maximumRows must be between 1 and 50");
        }
    }

    public static void followUp(long customerId, String actionType, String notes, String requestedBy) {
        if (customerId < 1) throw new IllegalArgumentException("customerId must be positive");
        if (!ACTION_TYPES.contains(actionType)) throw new IllegalArgumentException("Unsupported actionType");
        if (notes == null || notes.trim().length() < 3 || notes.length() > 2000) {
            throw new IllegalArgumentException("actionNotes must contain 3 to 2000 characters");
        }
        if (requestedBy == null || requestedBy.trim().length() < 3 || requestedBy.length() > 200) {
            throw new IllegalArgumentException("requestedBy must contain 3 to 200 characters");
        }
    }
}
