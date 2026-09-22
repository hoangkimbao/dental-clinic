package com.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu hiện tại không được để trống!")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống!")
    @Size(min = 3, message = "Mật khẩu mới phải có ít nhất 3 ký tự!")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
