package com.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc số điện thoại!")
    private String identifier;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
}
