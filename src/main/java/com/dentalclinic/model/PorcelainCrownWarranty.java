package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "porcelain_crown_warranties", indexes = {
    @Index(name = "idx_warranty_code", columnList = "warrantyCode"),
    @Index(name = "idx_warranty_qr", columnList = "qrCodeString"),
    @Index(name = "idx_warranty_phone", columnList = "patientPhone")
})
public class PorcelainCrownWarranty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String warrantyCode;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String patientPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrownType crownType;

    @Column(nullable = false)
    private String toothPositions;

    private String laboOrigin;

    private Integer warrantyYears;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(unique = true)
    private String qrCodeString;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyStatus status = WarrantyStatus.ACTIVE;

    public PorcelainCrownWarranty() {}

    public PorcelainCrownWarranty(String warrantyCode, String patientName, String patientPhone, CrownType crownType,
                                   String toothPositions, String laboOrigin, Integer warrantyYears,
                                   LocalDate startDate, LocalDate endDate, String qrCodeString, WarrantyStatus status) {
        this.warrantyCode = warrantyCode;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.crownType = crownType;
        this.toothPositions = toothPositions;
        this.laboOrigin = laboOrigin;
        this.warrantyYears = warrantyYears;
        this.startDate = startDate;
        this.endDate = endDate;
        this.qrCodeString = qrCodeString;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWarrantyCode() { return warrantyCode; }
    public void setWarrantyCode(String warrantyCode) { this.warrantyCode = warrantyCode; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public CrownType getCrownType() { return crownType; }
    public void setCrownType(CrownType crownType) { this.crownType = crownType; }

    public String getToothPositions() { return toothPositions; }
    public void setToothPositions(String toothPositions) { this.toothPositions = toothPositions; }

    public String getLaboOrigin() { return laboOrigin; }
    public void setLaboOrigin(String laboOrigin) { this.laboOrigin = laboOrigin; }

    public Integer getWarrantyYears() { return warrantyYears; }
    public void setWarrantyYears(Integer warrantyYears) { this.warrantyYears = warrantyYears; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getQrCodeString() { return qrCodeString; }
    public void setQrCodeString(String qrCodeString) { this.qrCodeString = qrCodeString; }

    public WarrantyStatus getStatus() { return status; }
    public void setStatus(WarrantyStatus status) { this.status = status; }
}
