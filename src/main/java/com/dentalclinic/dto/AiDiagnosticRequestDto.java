package com.dentalclinic.dto;

public class AiDiagnosticRequestDto {
    private String patientName;
    private String patientPhone;
    private String imageUrl;
    private String symptoms;

    public AiDiagnosticRequestDto() {}

    public AiDiagnosticRequestDto(String patientName, String patientPhone, String imageUrl, String symptoms) {
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.imageUrl = imageUrl;
        this.symptoms = symptoms;
    }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
}
