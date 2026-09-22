package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "medical_records")
public class MedicalRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chuẩn hóa liên kết trực tiếp tới User Bệnh nhân
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;

    private String patientName;
    private String patientPhone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dentist_id")
    private User dentist;

    @Column(length = 1000)
    private String diagnosis;

    @Column(length = 2000)
    private String treatmentDone;

    @Column(length = 1000)
    private String prescription;

    private LocalDate recordDate;
    private LocalDate nextFollowUpDate;
    private String notes;

    public MedicalRecord() {
        this.recordDate = LocalDate.now();
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

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getTreatmentDone() { return treatmentDone; }
    public void setTreatmentDone(String treatmentDone) { this.treatmentDone = treatmentDone; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public LocalDate getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDate nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
