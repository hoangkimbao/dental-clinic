package com.dentalclinic.dto;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private String phone;
    private String email;

    public AuthResponse(String token, Long id, String username, String fullName, String role, String phone, String email) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
