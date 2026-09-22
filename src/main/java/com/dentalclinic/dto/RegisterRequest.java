package com.dentalclinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Họ và tên không được để trống!")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^(0[3|5|7|8|9])([0-9]{8})$", message = "Số điện thoại không hợp lệ! Vui lòng nhập 10 chữ số (VD: 0912345678).")
    private String phone;

    @Email(message = "Email không đúng định dạng!")
    private String email;

    @NotBlank(message = "Tên đăng nhập không được để trống!")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự!")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 3, max = 100, message = "Mật khẩu phải có ít nhất 3 ký tự!")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
