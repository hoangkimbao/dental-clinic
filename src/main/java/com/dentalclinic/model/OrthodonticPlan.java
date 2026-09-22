package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "orthodontic_plans")
public class OrthodonticPlan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chuẩn hóa liên kết User
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;

    private String patientName;
    private String patientPhone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dentist_id")
    private User dentist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BracketType bracketType = BracketType.DAMON_Q2_METAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrthoStage currentStage = OrthoStage.GAN_MAC_CAI;

    private int totalEstimatedMonths;
    private int completedMonths;
    private LocalDate startDate;
    private LocalDate nextAdjustmentDate;

    @Column(length = 2000)
    private String doctorNotes;

    public OrthodonticPlan() {
        this.startDate = LocalDate.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) {
        this.patient = patient;
        if (patient != null) {
            this.patientName = patient.getFullName();
            this.patientPhone = patient.getPhone();
        }
    }

    public String getPatientName() { return patient != null ? patient.getFullName() : patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patient != null ? patient.getPhone() : patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public User getDentist() { return dentist; }
    public void setDentist(User dentist) { this.dentist = dentist; }

    public BracketType getBracketType() { return bracketType; }
    public void setBracketType(BracketType bracketType) { this.bracketType = bracketType; }

    public OrthoStage getCurrentStage() { return currentStage; }
    public void setCurrentStage(OrthoStage currentStage) { this.currentStage = currentStage; }

    public int getTotalEstimatedMonths() { return totalEstimatedMonths; }
    public void setTotalEstimatedMonths(int totalEstimatedMonths) { this.totalEstimatedMonths = totalEstimatedMonths; }

    public int getCompletedMonths() { return completedMonths; }
    public void setCompletedMonths(int completedMonths) { this.completedMonths = completedMonths; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getNextAdjustmentDate() { return nextAdjustmentDate; }
    public void setNextAdjustmentDate(LocalDate nextAdjustmentDate) { this.nextAdjustmentDate = nextAdjustmentDate; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
}
