package com.dentalclinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_reviews")
public class DoctorReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long dentistId;
    
    private String dentistName;

    private String patientName;
    
    private String patientPhone;

    @Column(nullable = false)
    private Integer rating = 5;

    private String serviceUsed;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String verifiedBadge;

    private LocalDateTime createdAt;

    public DoctorReview() {
        this.rating = 5;
        this.verifiedBadge = "Khách hàng đã khám thực tế";
    }

    public DoctorReview(Long dentistId, String patientName, Integer rating, String comment, String serviceUsed, boolean verified) {
        this.dentistId = dentistId;
        this.patientName = patientName;
        this.rating = rating != null ? rating : 5;
        this.comment = comment;
        this.serviceUsed = serviceUsed;
        this.verifiedBadge = verified ? "Khách hàng đã khám thực tế" : "";
    }

    public DoctorReview(Long id, Long dentistId, String dentistName, String patientName, String patientPhone, Integer rating, String serviceUsed, String comment, String verifiedBadge, LocalDateTime createdAt) {
        this.id = id;
        this.dentistId = dentistId;
        this.dentistName = dentistName;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.rating = rating != null ? rating : 5;
        this.serviceUsed = serviceUsed;
        this.comment = comment;
        this.verifiedBadge = verifiedBadge;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (rating == null) rating = 5;
        if (verifiedBadge == null) verifiedBadge = "Khách hàng đã khám thực tế";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDentistId() { return dentistId; }
    public void setDentistId(Long dentistId) { this.dentistId = dentistId; }

    public String getDentistName() { return dentistName; }
    public void setDentistName(String dentistName) { this.dentistName = dentistName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getServiceUsed() { return serviceUsed; }
    public void setServiceUsed(String serviceUsed) { this.serviceUsed = serviceUsed; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getVerifiedBadge() { return verifiedBadge; }
    public void setVerifiedBadge(String verifiedBadge) { this.verifiedBadge = verifiedBadge; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
