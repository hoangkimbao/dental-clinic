package com.dentalclinic.dto;

import com.dentalclinic.model.DentalPathology;
import java.time.LocalDateTime;

public class AiDiagnosticResponseDto {
    private Long logId;
    private String patientName;
    private DentalPathology detectedPathology;
    private String pathologyNameVi;
    private String severityLevel;
    private Double confidenceScore;
    private String clinicalRecommendation;
    private String recommendedServiceCode;
    private LocalDateTime analyzedAt;

    public AiDiagnosticResponseDto() {}

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public DentalPathology getDetectedPathology() { return detectedPathology; }
    public void setDetectedPathology(DentalPathology detectedPathology) { this.detectedPathology = detectedPathology; }

    public String getPathologyNameVi() { return pathologyNameVi; }
    public void setPathologyNameVi(String pathologyNameVi) { this.pathologyNameVi = pathologyNameVi; }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getClinicalRecommendation() { return clinicalRecommendation; }
    public void setClinicalRecommendation(String clinicalRecommendation) { this.clinicalRecommendation = clinicalRecommendation; }

    public String getRecommendedServiceCode() { return recommendedServiceCode; }
    public void setRecommendedServiceCode(String recommendedServiceCode) { this.recommendedServiceCode = recommendedServiceCode; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
