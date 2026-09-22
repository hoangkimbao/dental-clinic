package com.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCouponRequest {
    @NotBlank(message = "Mã ưu đãi không được để trống!")
    @Size(min = 3, max = 30, message = "Mã ưu đãi phải từ 3 đến 30 ký tự!")
    private String code;

    @NotBlank(message = "Tiêu đề ưu đãi không được để trống!")
    private String title;

    private String description;

    @NotBlank(message = "Loại giảm giá không được để trống!")
    private String discountType; // FIXED_AMOUNT, PERCENTAGE

    @NotNull(message = "Mức giảm giá không được để trống!")
    private Double discountValue;

    private String applicableService = "ALL";
    private Integer validDays = 30;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public String getApplicableService() { return applicableService; }
    public void setApplicableService(String applicableService) { this.applicableService = applicableService; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
}
