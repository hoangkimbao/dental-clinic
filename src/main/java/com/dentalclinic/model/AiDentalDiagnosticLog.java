package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_dental_diagnostic_logs", indexes = {
    @Index(name = "idx_diag_phone", columnList = "patientPhone"),
    @Index(name = "idx_diag_analyzed", columnList = "analyzedAt")
})
public class AiDentalDiagnosticLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientName;

    private String patientPhone;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DentalPathology detectedPathology;

    private String severityLevel; // MILD, MODERATE, SEVERE, NONE

    private Double confidenceScore;

    @Column(length = 2000)
    private String clinicalRecommendation;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    public AiDentalDiagnosticLog() {
        this.analyzedAt = LocalDateTime.now();
    }

    public AiDentalDiagnosticLog(String patientName, String patientPhone, String imageUrl,
                                 DentalPathology detectedPathology, String severityLevel,
                                 Double confidenceScore, String clinicalRecommendation, LocalDateTime analyzedAt) {
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.imageUrl = imageUrl;
        this.detectedPathology = detectedPathology;
        this.severityLevel = severityLevel;
        this.confidenceScore = confidenceScore;
        this.clinicalRecommendation = clinicalRecommendation;
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public DentalPathology getDetectedPathology() { return detectedPathology; }
    public void setDetectedPathology(DentalPathology detectedPathology) { this.detectedPathology = detectedPathology; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getClinicalRecommendation() { return clinicalRecommendation; }
    public void setClinicalRecommendation(String clinicalRecommendation) { this.clinicalRecommendation = clinicalRecommendation; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
