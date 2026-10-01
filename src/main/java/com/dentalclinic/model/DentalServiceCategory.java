package com.dentalclinic.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DentalServiceCategory {
    ORTHODONTICS,
    IMPLANT,
    PORCELAIN_CROWNS,
    WHITENING,
    WISDOM_TEETH,
    GENERAL;

    @JsonCreator
    public static DentalServiceCategory fromValue(String value) {
        if (value == null) return null;
        switch (value.toUpperCase()) {
            case "CHINH_NHA":
            case "ORTHODONTICS": return ORTHODONTICS;
            case "IMPLANT":
            case "CAY_GHEP_IMPLANT": return IMPLANT;
            case "PORCELAIN_CROWNS":
            case "BOC_RANG_SU":
            case "RANG_SU": return PORCELAIN_CROWNS;
            case "WHITENING":
            case "TAY_TRANG_RANG": return WHITENING;
            case "WISDOM_TEETH":
            case "RANG_KHON": return WISDOM_TEETH;
            case "GENERAL":
            case "TONG_QUAT": return GENERAL;
            default:
                try { return valueOf(value.toUpperCase()); }
                catch (IllegalArgumentException e) { return GENERAL; }
        }
    }
}
