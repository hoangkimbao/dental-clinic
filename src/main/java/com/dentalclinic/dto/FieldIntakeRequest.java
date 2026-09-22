package com.dentalclinic.dto;

import com.dentalclinic.model.FieldEventType;

public class FieldIntakeRequest {
    private String eventName;
    private FieldEventType eventType;
    private String eventLocation;
    private String patientFullName;
    private String patientName;
    private Integer birthYear;
    private Integer age;
    private String studentClass;
    private String phone;
    private String email;
    private String parentName;
    private String parentPhone;
    private String screeningFindings;
    private String initialComplaint;
    private String recommendation;
    private String preliminaryPathology;
    private String voucherCode;
    private Long branchId;
    private String notes;

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public FieldEventType getEventType() { return eventType; }
    public void setEventType(FieldEventType eventType) { this.eventType = eventType; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public String getPatientFullName() {
        return patientFullName != null ? patientFullName : patientName;
    }
    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
        if (this.patientName == null) this.patientName = patientFullName;
    }

    public String getPatientName() {
        return patientName != null ? patientName : patientFullName;
    }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
        if (this.patientFullName == null) this.patientFullName = patientName;
    }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getStudentClass() { return studentClass; }
    public void setStudentClass(String studentClass) { this.studentClass = studentClass; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getScreeningFindings() {
        return screeningFindings != null ? screeningFindings : initialComplaint;
    }
    public void setScreeningFindings(String screeningFindings) {
        this.screeningFindings = screeningFindings;
        if (this.initialComplaint == null) this.initialComplaint = screeningFindings;
    }

    public String getInitialComplaint() {
        return initialComplaint != null ? initialComplaint : screeningFindings;
    }
    public void setInitialComplaint(String initialComplaint) {
        this.initialComplaint = initialComplaint;
        if (this.screeningFindings == null) this.screeningFindings = initialComplaint;
    }

    public String getRecommendation() {
        return recommendation != null ? recommendation : preliminaryPathology;
    }
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
        if (this.preliminaryPathology == null) this.preliminaryPathology = recommendation;
    }

    public String getPreliminaryPathology() {
        return preliminaryPathology != null ? preliminaryPathology : recommendation;
    }
    public void setPreliminaryPathology(String preliminaryPathology) {
        this.preliminaryPathology = preliminaryPathology;
        if (this.recommendation == null) this.recommendation = preliminaryPathology;
    }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
