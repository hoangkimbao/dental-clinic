package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "doctor_kpi_records", indexes = {
    @Index(name = "idx_doc_kpi_doctor", columnList = "doctor_id"),
    @Index(name = "idx_doc_kpi_month", columnList = "period_month")
})
public class DoctorKpiRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate = LocalDate.now();

    @Column(name = "period_month", nullable = false)
    private String periodMonth;

    @Column(name = "total_consultations", nullable = false)
    private Integer totalConsultations = 0;

    @Column(name = "completed_consultations", nullable = false)
    private Integer completedConsultations = 0;

    @Column(name = "new_patients_examined", nullable = false)
    private Integer newPatientsExamined = 0;

    @Column(name = "ortho_cases_started", nullable = false)
    private Integer orthoCasesStarted = 0;

    @Column(name = "implant_cases_started", nullable = false)
    private Integer implantCasesStarted = 0;

    @Column(name = "general_cases_completed", nullable = false)
    private Integer generalCasesCompleted = 0;

    @Column(name = "revenue_generated", nullable = false)
    private Double revenueGenerated = 0.0;

    @Column(name = "total_revenue_attributed", nullable = false)
    private Double totalRevenueAttributed = 0.0;

    @Column(name = "kpi_score", nullable = false)
    private Double kpiScore = 0.0;

    @Transient
    private String doctorName;

    public DoctorKpiRecord() {}

    public DoctorKpiRecord(User doctor, LocalDate recordDate, String periodMonth,
                           Integer totalConsultations, Integer newPatientsExamined,
                           Integer orthoCasesStarted, Integer implantCasesStarted,
                           Integer generalCasesCompleted, Double revenueGenerated,
                           Double kpiScore) {
        this.doctor = doctor;
        this.recordDate = recordDate != null ? recordDate : LocalDate.now();
        this.periodMonth = periodMonth;
        this.totalConsultations = totalConsultations != null ? totalConsultations : 0;
        this.completedConsultations = this.totalConsultations;
        this.newPatientsExamined = newPatientsExamined != null ? newPatientsExamined : 0;
        this.orthoCasesStarted = orthoCasesStarted != null ? orthoCasesStarted : 0;
        this.implantCasesStarted = implantCasesStarted != null ? implantCasesStarted : 0;
        this.generalCasesCompleted = generalCasesCompleted != null ? generalCasesCompleted : 0;
        this.revenueGenerated = revenueGenerated != null ? revenueGenerated : 0.0;
        this.totalRevenueAttributed = this.revenueGenerated;
        this.kpiScore = kpiScore != null ? kpiScore : 0.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }

    public Integer getTotalConsultations() { return totalConsultations != null ? totalConsultations : 0; }
    public void setTotalConsultations(Integer totalConsultations) {
        this.totalConsultations = totalConsultations;
        this.completedConsultations = totalConsultations;
    }

    public Integer getCompletedConsultations() {
        return completedConsultations != null ? completedConsultations : getTotalConsultations();
    }
    public void setCompletedConsultations(Integer completedConsultations) {
        this.completedConsultations = completedConsultations;
        this.totalConsultations = completedConsultations;
    }

    public Integer getNewPatientsExamined() { return newPatientsExamined != null ? newPatientsExamined : 0; }
    public void setNewPatientsExamined(Integer newPatientsExamined) { this.newPatientsExamined = newPatientsExamined; }

    public Integer getOrthoCasesStarted() { return orthoCasesStarted != null ? orthoCasesStarted : 0; }
    public void setOrthoCasesStarted(Integer orthoCasesStarted) { this.orthoCasesStarted = orthoCasesStarted; }

    public Integer getImplantCasesStarted() { return implantCasesStarted != null ? implantCasesStarted : 0; }
    public void setImplantCasesStarted(Integer implantCasesStarted) { this.implantCasesStarted = implantCasesStarted; }

    public Integer getGeneralCasesCompleted() { return generalCasesCompleted != null ? generalCasesCompleted : 0; }
    public void setGeneralCasesCompleted(Integer generalCasesCompleted) { this.generalCasesCompleted = generalCasesCompleted; }

    public Double getRevenueGenerated() { return revenueGenerated != null ? revenueGenerated : 0.0; }
    public void setRevenueGenerated(Double revenueGenerated) {
        this.revenueGenerated = revenueGenerated;
        this.totalRevenueAttributed = revenueGenerated;
    }

    public Double getTotalRevenueAttributed() {
        return totalRevenueAttributed != null ? totalRevenueAttributed : getRevenueGenerated();
    }
    public void setTotalRevenueAttributed(Double totalRevenueAttributed) {
        this.totalRevenueAttributed = totalRevenueAttributed;
        this.revenueGenerated = totalRevenueAttributed;
    }

    public Double getKpiScore() { return kpiScore != null ? kpiScore : 0.0; }
    public void setKpiScore(Double kpiScore) { this.kpiScore = kpiScore; }

    public String getDoctorName() {
        if (doctorName != null) return doctorName;
        return (doctor != null) ? doctor.getFullName() : "BS. Chưa phân công";
    }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    @JsonProperty("treatmentConversions")
    public Map<String, Object> getTreatmentConversions() {
        Map<String, Object> map = new HashMap<>();
        int ortho = getOrthoCasesStarted();
        int implant = getImplantCasesStarted();
        int general = getGeneralCasesCompleted();
        int total = getCompletedConsultations();
        double conversionRate = (total > 0) ? Math.min(100.0, Math.round(((double)(ortho + implant) / total) * 1000.0) / 10.0) : 0.0;

        map.put("orthoCases", ortho);
        map.put("implantCases", implant);
        map.put("generalCases", general);
        map.put("conversionRatePercent", conversionRate);
        return map;
    }
}
