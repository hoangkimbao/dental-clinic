package com.dentalclinic.dto;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DoctorKpiResponseDto {
    private Long doctorId;
    private String doctorName;
    private LocalDate recordDate;
    private String periodMonth;
    private Integer completedConsultations = 0;
    private Integer totalConsultations = 0;
    private Integer newPatientsExamined = 0;
    private Integer orthoCasesStarted = 0;
    private Integer implantCasesStarted = 0;
    private Integer generalCasesCompleted = 0;
    private Double totalRevenueAttributed = 0.0;
    private Double revenueGenerated = 0.0;
    private Double kpiScore = 0.0;
    private Map<String, Object> treatmentConversions = new HashMap<>();

    public DoctorKpiResponseDto() {}

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }

    public Integer getCompletedConsultations() {
        return completedConsultations != null ? completedConsultations : totalConsultations;
    }
    public void setCompletedConsultations(Integer completedConsultations) {
        this.completedConsultations = completedConsultations;
        if (this.totalConsultations == null || this.totalConsultations == 0) {
            this.totalConsultations = completedConsultations;
        }
    }

    public Integer getTotalConsultations() {
        return totalConsultations != null ? totalConsultations : completedConsultations;
    }
    public void setTotalConsultations(Integer totalConsultations) {
        this.totalConsultations = totalConsultations;
        if (this.completedConsultations == null || this.completedConsultations == 0) {
            this.completedConsultations = totalConsultations;
        }
    }

    public Integer getNewPatientsExamined() { return newPatientsExamined; }
    public void setNewPatientsExamined(Integer newPatientsExamined) { this.newPatientsExamined = newPatientsExamined; }

    public Integer getOrthoCasesStarted() { return orthoCasesStarted; }
    public void setOrthoCasesStarted(Integer orthoCasesStarted) { this.orthoCasesStarted = orthoCasesStarted; }

    public Integer getImplantCasesStarted() { return implantCasesStarted; }
    public void setImplantCasesStarted(Integer implantCasesStarted) { this.implantCasesStarted = implantCasesStarted; }

    public Integer getGeneralCasesCompleted() { return generalCasesCompleted; }
    public void setGeneralCasesCompleted(Integer generalCasesCompleted) { this.generalCasesCompleted = generalCasesCompleted; }

    public Double getTotalRevenueAttributed() {
        return totalRevenueAttributed != null ? totalRevenueAttributed : revenueGenerated;
    }
    public void setTotalRevenueAttributed(Double totalRevenueAttributed) {
        this.totalRevenueAttributed = totalRevenueAttributed;
        if (this.revenueGenerated == null || this.revenueGenerated == 0.0) {
            this.revenueGenerated = totalRevenueAttributed;
        }
    }

    public Double getRevenueGenerated() {
        return revenueGenerated != null ? revenueGenerated : totalRevenueAttributed;
    }
    public void setRevenueGenerated(Double revenueGenerated) {
        this.revenueGenerated = revenueGenerated;
        if (this.totalRevenueAttributed == null || this.totalRevenueAttributed == 0.0) {
            this.totalRevenueAttributed = revenueGenerated;
        }
    }

    public Double getKpiScore() { return kpiScore; }
    public void setKpiScore(Double kpiScore) { this.kpiScore = kpiScore; }

    public Map<String, Object> getTreatmentConversions() { return treatmentConversions; }
    public void setTreatmentConversions(Map<String, Object> treatmentConversions) {
        this.treatmentConversions = treatmentConversions;
    }
}
