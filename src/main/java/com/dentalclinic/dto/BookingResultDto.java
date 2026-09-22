package com.dentalclinic.dto;

import com.dentalclinic.model.Appointment;

public class BookingResultDto {
    private Appointment appointment;
    private boolean newAccountCreated;
    private String generatedUsername;
    private String generatedPassword;
    private AuthResponse authInfo;

    public BookingResultDto() {}

    public BookingResultDto(Appointment appointment, boolean newAccountCreated, String generatedUsername, String generatedPassword, AuthResponse authInfo) {
        this.appointment = appointment;
        this.newAccountCreated = newAccountCreated;
        this.generatedUsername = generatedUsername;
        this.generatedPassword = generatedPassword;
        this.authInfo = authInfo;
    }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public boolean isNewAccountCreated() { return newAccountCreated; }
    public void setNewAccountCreated(boolean newAccountCreated) { this.newAccountCreated = newAccountCreated; }
    public String getGeneratedUsername() { return generatedUsername; }
    public void setGeneratedUsername(String generatedUsername) { this.generatedUsername = generatedUsername; }
    public String getGeneratedPassword() { return generatedPassword; }
    public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }
    public AuthResponse getAuthInfo() { return authInfo; }
    public void setAuthInfo(AuthResponse authInfo) { this.authInfo = authInfo; }
}
