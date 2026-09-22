package com.dentalclinic.service;

import com.dentalclinic.dto.BranchDistanceDto;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.ClinicBranch;
import com.dentalclinic.repository.ClinicBranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ClinicBranchService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ClinicBranchRepository branchRepository;

    public ClinicBranchService(ClinicBranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicBranch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ClinicBranch getBranchByCode(String code) {
        return branchRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Chi nhánh không tồn tại với mã: " + code));
    }

    @Transactional(readOnly = true)
    public List<BranchDistanceDto> findNearestBranches(double userLat, double userLng) {
        List<ClinicBranch> branches = branchRepository.findAll();
        List<BranchDistanceDto> result = new ArrayList<>();

        for (ClinicBranch b : branches) {
            double dist = 0.0;
            if (b.getLatitude() != null && b.getLongitude() != null) {
                dist = calculateHaversineDistance(userLat, userLng, b.getLatitude(), b.getLongitude());
            }
            String mapsUrl = "https://www.google.com/maps/dir/?api=1&destination=" + b.getLatitude() + "," + b.getLongitude();
            result.add(new BranchDistanceDto(b, Math.round(dist * 10.0) / 10.0, mapsUrl));
        }

        result.sort(Comparator.comparingDouble(BranchDistanceDto::getDistanceKm));
        return result;
    }

    public ClinicBranch saveBranch(ClinicBranch branch) {
        return branchRepository.save(branch);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
