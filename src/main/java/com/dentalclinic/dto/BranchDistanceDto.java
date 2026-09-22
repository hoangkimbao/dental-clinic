package com.dentalclinic.dto;

import com.dentalclinic.model.ClinicBranch;

public class BranchDistanceDto {
    private ClinicBranch branch;
    private Double distanceKm;
    private String googleMapsUrl;

    public BranchDistanceDto() {}

    public BranchDistanceDto(ClinicBranch branch, Double distanceKm, String googleMapsUrl) {
        this.branch = branch;
        this.distanceKm = distanceKm;
        this.googleMapsUrl = googleMapsUrl;
    }

    public ClinicBranch getBranch() { return branch; }
    public void setBranch(ClinicBranch branch) { this.branch = branch; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getGoogleMapsUrl() { return googleMapsUrl; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }
}
