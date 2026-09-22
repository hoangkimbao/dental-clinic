package com.dentalclinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class BookingRequest {
    @NotBlank(message = "Họ tên bệnh nhân không được để trống!")
    private String patientName;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^(0[3|5|7|8|9])([0-9]{8})$", message = "Số điện thoại không hợp lệ! Vui lòng nhập 10 chữ số.")
    private String patientPhone;

    private String patientEmail;

    @NotBlank(message = "Dịch vụ khám không được để trống!")
    private String serviceName;

    private String appointmentTime;
    private Long dentistId;
    private String notes;
    private String couponCode;

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public Long getDentistId() { return dentistId; }
    public void setDentistId(Long dentistId) { this.dentistId = dentistId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
