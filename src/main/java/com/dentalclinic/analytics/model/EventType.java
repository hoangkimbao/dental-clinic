package com.dentalclinic.analytics.model;

/**
 * Supported User Behavior Analytics Event Types.
 */
public enum EventType {
    PAGE_VIEW,
    CLICK,
    BOOKING_FUNNEL,
    ORDER_FUNNEL,
    SEARCH,
    QR_SCAN,
    NAVIGATION;

    public static EventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return PAGE_VIEW;
        }
        try {
            return EventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PAGE_VIEW;
        }
    }
}
