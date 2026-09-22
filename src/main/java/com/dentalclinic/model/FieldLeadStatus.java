package com.dentalclinic.model;

public enum FieldLeadStatus {
    NEW,
    NEW_LEAD,
    CONTACTED,
    BOOKED,
    BOOKED_APPOINTMENT,
    VISITED,
    CONVERTED;

    public static FieldLeadStatus fromString(String val) {
        if (val == null || val.trim().isEmpty()) {
            return NEW;
        }
        String clean = val.trim().toUpperCase();
        for (FieldLeadStatus status : values()) {
            if (status.name().equals(clean)) {
                return status;
            }
        }
        if (clean.contains("BOOK")) {
            return BOOKED_APPOINTMENT;
        }
        if (clean.contains("CONTACT")) {
            return CONTACTED;
        }
        if (clean.contains("CONVERT")) {
            return CONVERTED;
        }
        return NEW;
    }
}
