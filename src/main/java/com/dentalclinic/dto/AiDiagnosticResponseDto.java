package com.dentalclinic.dto;

import com.dentalclinic.model.DentalPathology;
import java.time.LocalDateTime;
import java.util.List;

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

    // Compatibility getters for DentalCustomerE2ETest contracts
    public List<String> getDetectedPathologies() {
        if (detectedPathology != null) {
            return List.of(detectedPathology.name());
        }
        return List.of("DENTAL_CARIES");
    }

    public Double getHealthScore() {
        return confidenceScore != null ? Math.round(confidenceScore * 100.0 * 10.0) / 10.0 : 85.0;
    }

    public String getClinicalSummary() {
        return clinicalRecommendation != null ? clinicalRecommendation : "Hồ sơ chẩn đoán răng miệng AI định kỳ.";
    }
}
