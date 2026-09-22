package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_app_status", columnList = "status"),
    @Index(name = "idx_app_time", columnList = "appointmentTime")
})
public class Appointment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chuẩn hóa: Liên kết trực tiếp tới Entity User (Bệnh nhân)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id")
    private User patient;

    // Snapshot fallback cho khách vãng lai
    private String patientName;
    private String patientPhone;
    private String patientEmail;

    @Column(nullable = false)
    private String serviceName;

    private LocalDateTime appointmentTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dentist_id")
    private User dentist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private Double depositAmount;
    private String notes;

    // Optimistic Locking chống đặt trùng lịch
    @Version
    private Long version;

    public Appointment() {
        this.status = AppointmentStatus.PENDING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) {
        this.patient = patient;
        if (patient != null) {
            this.patientName = patient.getFullName();
            this.patientPhone = patient.getPhone();
            this.patientEmail = patient.getEmail();
        }
    }

    public String getPatientName() { return patient != null ? patient.getFullName() : patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patient != null ? patient.getPhone() : patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getPatientEmail() { return patient != null ? patient.getEmail() : patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }

    public User getDentist() { return dentist; }
    public void setDentist(User dentist) { this.dentist = dentist; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public Double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Double depositAmount) { this.depositAmount = depositAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
