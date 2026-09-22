package com.dentalclinic.dto;

public class CouponResultDto {
    private String code;
    private String title;
    private String discountDescription;
    private Double discountAmount;

    public CouponResultDto(String code, String title, String discountDescription, Double discountAmount) {
        this.code = code;
        this.title = title;
        this.discountDescription = discountDescription;
        this.discountAmount = discountAmount;
    }

    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDiscountDescription() { return discountDescription; }
    public Double getDiscountAmount() { return discountAmount; }
}
