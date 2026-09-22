package com.dentalclinic.dto;

public class AttendanceCheckInRequest {
    private Long shiftId;
    private Double latitude;
    private Double longitude;
    private String networkIp;
    private String checkInIp;

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getNetworkIp() {
        return networkIp != null ? networkIp : checkInIp;
    }
    public void setNetworkIp(String networkIp) {
        this.networkIp = networkIp;
        if (this.checkInIp == null) this.checkInIp = networkIp;
    }

    public String getCheckInIp() {
        return checkInIp != null ? checkInIp : networkIp;
    }
    public void setCheckInIp(String checkInIp) {
        this.checkInIp = checkInIp;
        if (this.networkIp == null) this.networkIp = checkInIp;
    }
}
