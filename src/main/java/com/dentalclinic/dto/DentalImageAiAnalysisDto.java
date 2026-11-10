package com.dentalclinic.dto;

import java.time.LocalDateTime;

public class DentalImageAiAnalysisDto {
    private Long imageId;
    private String patientName;
    private String imageType;
    private String fileUrl;
    private String findings;
    private String riskLevel;
    private String clinicalRecommendation;
    private String aiModelUsed;
    private String disclaimer;
    private LocalDateTime analyzedAt;

    public DentalImageAiAnalysisDto() {
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getClinicalRecommendation() {
        return clinicalRecommendation;
    }

    public void setClinicalRecommendation(String clinicalRecommendation) {
        this.clinicalRecommendation = clinicalRecommendation;
    }

    public String getAiModelUsed() {
        return aiModelUsed;
    }

    public void setAiModelUsed(String aiModelUsed) {
        this.aiModelUsed = aiModelUsed;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
