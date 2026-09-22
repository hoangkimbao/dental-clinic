package com.dentalclinic.dto;

import com.dentalclinic.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StaffCreateRequest {
    @NotBlank(message = "Họ tên nhân viên không được để trống!")
    private String fullName;

    private String phone;

    @NotNull(message = "Vai trò nhân viên không được để trống!")
    private Role role;

    @NotBlank(message = "Tên đăng nhập không được để trống!")
    @Size(min = 3, message = "Tên đăng nhập phải từ 3 ký tự!")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    @Size(min = 3, message = "Mật khẩu phải từ 3 ký tự!")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
