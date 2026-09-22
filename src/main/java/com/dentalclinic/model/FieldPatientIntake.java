package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "field_patient_intakes", indexes = {
    @Index(name = "idx_field_lead_code", columnList = "lead_code", unique = true),
    @Index(name = "idx_field_phone", columnList = "phone"),
    @Index(name = "idx_field_event", columnList = "event_name"),
    @Index(name = "idx_field_status", columnList = "status")
})
public class FieldPatientIntake extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_code", nullable = false, unique = true)
    private String leadCode;

    @Column(name = "event_name")
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private FieldEventType eventType = FieldEventType.SCHOOL_SCREENING;

    @Column(name = "event_location")
    private String eventLocation;

    @Column(name = "event_date")
    private LocalDate eventDate = LocalDate.now();

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_full_name")
    private String patientFullName;

    @Column(name = "birth_year")
    private Integer birthYear;

    private Integer age;

    @Column(name = "student_class")
    private String studentClass;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "initial_complaint", length = 1000)
    private String initialComplaint;

    @Column(name = "screening_findings", length = 1000)
    private String screeningFindings;

    @Column(name = "preliminary_pathology", length = 1000)
    private String preliminaryPathology;

    @Column(length = 1000)
    private String recommendation;

    @Column(name = "voucher_code")
    private String voucherCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id")
    private ClinicBranch assignedBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "captured_by_id")
    private User capturedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldLeadStatus status = FieldLeadStatus.NEW;

    @Column(length = 1000)
    private String notes;

    public FieldPatientIntake() {
        this.status = FieldLeadStatus.NEW;
        this.eventDate = LocalDate.now();
    }

    public FieldPatientIntake(String leadCode, String eventName, FieldEventType eventType,
                              String patientFullName, String phone, String screeningFindings,
                              String recommendation, String voucherCode) {
        this.leadCode = leadCode;
        this.eventName = eventName;
        this.eventType = eventType != null ? eventType : FieldEventType.SCHOOL_SCREENING;
        this.patientFullName = patientFullName;
        this.patientName = patientFullName;
        this.phone = phone;
        this.screeningFindings = screeningFindings;
        this.initialComplaint = screeningFindings;
        this.recommendation = recommendation;
        this.preliminaryPathology = recommendation;
        this.voucherCode = voucherCode;
        this.eventDate = LocalDate.now();
        this.status = FieldLeadStatus.NEW;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLeadCode() { return leadCode; }
    public void setLeadCode(String leadCode) { this.leadCode = leadCode; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public FieldEventType getEventType() { return eventType; }
    public void setEventType(FieldEventType eventType) { this.eventType = eventType; }

    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getPatientName() {
        return patientName != null ? patientName : patientFullName;
    }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
        if (this.patientFullName == null) this.patientFullName = patientName;
    }

    public String getPatientFullName() {
        return patientFullName != null ? patientFullName : patientName;
    }
    public void setPatientFullName(String patientFullName) {
        this.patientFullName = patientFullName;
        if (this.patientName == null) this.patientName = patientFullName;
    }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
        if (birthYear != null && birthYear > 1900) {
            this.age = LocalDate.now().getYear() - birthYear;
        }
    }

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

    public String getInitialComplaint() {
        return initialComplaint != null ? initialComplaint : screeningFindings;
    }
    public void setInitialComplaint(String initialComplaint) {
        this.initialComplaint = initialComplaint;
        if (this.screeningFindings == null) this.screeningFindings = initialComplaint;
    }

    public String getScreeningFindings() {
        return screeningFindings != null ? screeningFindings : initialComplaint;
    }
    public void setScreeningFindings(String screeningFindings) {
        this.screeningFindings = screeningFindings;
        if (this.initialComplaint == null) this.initialComplaint = screeningFindings;
    }

    public String getPreliminaryPathology() {
        return preliminaryPathology != null ? preliminaryPathology : recommendation;
    }
    public void setPreliminaryPathology(String preliminaryPathology) {
        this.preliminaryPathology = preliminaryPathology;
        if (this.recommendation == null) this.recommendation = preliminaryPathology;
    }

    public String getRecommendation() {
        return recommendation != null ? recommendation : preliminaryPathology;
    }
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
        if (this.preliminaryPathology == null) this.preliminaryPathology = recommendation;
    }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public ClinicBranch getAssignedBranch() { return assignedBranch; }
    public void setAssignedBranch(ClinicBranch assignedBranch) { this.assignedBranch = assignedBranch; }

    public User getCapturedBy() { return capturedBy; }
    public void setCapturedBy(User capturedBy) { this.capturedBy = capturedBy; }

    public FieldLeadStatus getStatus() { return status; }
    public void setStatus(FieldLeadStatus status) { this.status = status; }

    @JsonProperty("leadStatus")
    public String getLeadStatus() {
        return status != null ? status.name() : "NEW";
    }

    public void setLeadStatus(String leadStatus) {
        this.status = FieldLeadStatus.fromString(leadStatus);
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
