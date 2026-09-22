package com.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;

public class ValidateCouponRequest {
    @NotBlank(message = "Mã voucher không được để trống!")
    private String code;
    private String serviceName;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
}
