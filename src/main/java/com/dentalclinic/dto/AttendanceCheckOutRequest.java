package com.dentalclinic.dto;

public class AttendanceCheckOutRequest {
    private Long shiftId;
    private String notes;

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
