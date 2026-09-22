package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_attendances", indexes = {
    @Index(name = "idx_atn_staff", columnList = "staff_id"),
    @Index(name = "idx_atn_shift", columnList = "shift_id"),
    @Index(name = "idx_atn_status", columnList = "status")
})
public class StaffAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shift_id", nullable = false)
    private StaffShift shift;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "check_in_ip")
    private String checkInIp;

    @Column(name = "check_in_latitude")
    private Double checkInLatitude;

    @Column(name = "check_in_longitude")
    private Double checkInLongitude;

    @Column(name = "is_gps_verified")
    private Boolean isGpsVerified = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.ON_TIME;

    @Column(length = 1000)
    private String notes;

    public StaffAttendance() {}

    public StaffAttendance(User staff, StaffShift shift, LocalDateTime checkInTime,
                           String checkInIp, Double latitude, Double longitude,
                           Boolean isGpsVerified, AttendanceStatus status, String notes) {
        this.staff = staff;
        this.shift = shift;
        this.checkInTime = checkInTime;
        this.checkInIp = checkInIp;
        this.checkInLatitude = latitude;
        this.checkInLongitude = longitude;
        this.isGpsVerified = isGpsVerified != null ? isGpsVerified : (latitude != null && longitude != null);
        this.status = status != null ? status : AttendanceStatus.ON_TIME;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }

    public StaffShift getShift() { return shift; }
    public void setShift(StaffShift shift) { this.shift = shift; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getCheckInIp() { return checkInIp; }
    public void setCheckInIp(String checkInIp) { this.checkInIp = checkInIp; }

    // Alias networkIp
    public String getNetworkIp() { return checkInIp; }
    public void setNetworkIp(String networkIp) { this.checkInIp = networkIp; }

    public Double getCheckInLatitude() { return checkInLatitude; }
    public void setCheckInLatitude(Double checkInLatitude) { this.checkInLatitude = checkInLatitude; }

    // Alias latitude
    public Double getLatitude() { return checkInLatitude; }
    public void setLatitude(Double latitude) { this.checkInLatitude = latitude; }

    public Double getCheckInLongitude() { return checkInLongitude; }
    public void setCheckInLongitude(Double checkInLongitude) { this.checkInLongitude = checkInLongitude; }

    // Alias longitude
    public Double getLongitude() { return checkInLongitude; }
    public void setLongitude(Double longitude) { this.checkInLongitude = longitude; }

    public Boolean getIsGpsVerified() { return isGpsVerified != null ? isGpsVerified : true; }
    public void setIsGpsVerified(Boolean isGpsVerified) { this.isGpsVerified = isGpsVerified; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
