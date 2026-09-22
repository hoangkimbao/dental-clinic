package com.dentalclinic.dto;

public class DashboardStatsDto {
    private long totalAppointments;
    private long depositPaidCount;
    private long confirmedCount;
    private long completedCount;
    private long activeOrthoPlans;
    private long totalStaff;
    private double totalRevenueVND;

    public DashboardStatsDto(long totalAppointments, long depositPaidCount, long confirmedCount, long completedCount, long activeOrthoPlans, long totalStaff, double totalRevenueVND) {
        this.totalAppointments = totalAppointments;
        this.depositPaidCount = depositPaidCount;
        this.confirmedCount = confirmedCount;
        this.completedCount = completedCount;
        this.activeOrthoPlans = activeOrthoPlans;
        this.totalStaff = totalStaff;
        this.totalRevenueVND = totalRevenueVND;
    }

    public long getTotalAppointments() { return totalAppointments; }
    public long getDepositPaidCount() { return depositPaidCount; }
    public long getConfirmedCount() { return confirmedCount; }
    public long getCompletedCount() { return completedCount; }
    public long getActiveOrthoPlans() { return activeOrthoPlans; }
    public long getTotalStaff() { return totalStaff; }
    public double getTotalRevenueVND() { return totalRevenueVND; }
}
