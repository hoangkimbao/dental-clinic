package com.dentalclinic.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DentalProductCategory {
    BRUSH,
    FLOSSER,
    TOOTHPASTE,
    FLOSS,
    RETAINER;

    @JsonCreator
    public static DentalProductCategory fromValue(String value) {
        if (value == null) return null;
        switch (value.toUpperCase()) {
            case "BAN_CHAI_DIEN":
            case "BAN_CHAI":
            case "BRUSH": return BRUSH;
            case "FLOSSER":
            case "MAY_TAM_NUOC": return FLOSSER;
            case "TOOTHPASTE":
            case "KEM_DANH_RANG": return TOOTHPASTE;
            case "FLOSS":
            case "CHI_NHA_KHOA": return FLOSS;
            case "RETAINER":
            case "MANG_DUY_TRI": return RETAINER;
            default:
                try { return valueOf(value.toUpperCase()); }
                catch (IllegalArgumentException e) { return null; }
        }
    }
}
