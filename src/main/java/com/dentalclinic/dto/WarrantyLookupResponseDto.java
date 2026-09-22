package com.dentalclinic.dto;

import com.dentalclinic.model.CrownType;
import com.dentalclinic.model.WarrantyStatus;
import java.time.LocalDate;

public class WarrantyLookupResponseDto {
    private String warrantyCode;
    private String patientName;
    private String patientPhoneMasked;
    private CrownType crownType;
    private String crownTypeName;
    private String toothPositions;
    private String laboOrigin;
    private Integer warrantyYears;
    private LocalDate startDate;
    private LocalDate endDate;
    private String qrCodeString;
    private WarrantyStatus status;
    private boolean valid;
    private long remainingDays;

    public WarrantyLookupResponseDto() {}

    public String getWarrantyCode() { return warrantyCode; }
    public void setWarrantyCode(String warrantyCode) { this.warrantyCode = warrantyCode; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhoneMasked() { return patientPhoneMasked; }
    public void setPatientPhoneMasked(String patientPhoneMasked) { this.patientPhoneMasked = patientPhoneMasked; }

    public CrownType getCrownType() { return crownType; }
    public void setCrownType(CrownType crownType) { this.crownType = crownType; }

    public String getCrownTypeName() { return crownTypeName; }
    public void setCrownTypeName(String crownTypeName) { this.crownTypeName = crownTypeName; }

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

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }
}
