package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.BranchDistanceDto;
import com.dentalclinic.model.ClinicBranch;
import com.dentalclinic.service.ClinicBranchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class ClinicBranchController {

    private final ClinicBranchService branchService;

    public ClinicBranchController(ClinicBranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ApiResponse<List<ClinicBranch>> getAllBranches() {
        return ApiResponse.success(branchService.getAllBranches());
    }

    @GetMapping("/nearest")
    public ApiResponse<?> getNearestBranches(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        double actualLat = (latitude != null) ? latitude : (lat != null ? lat : 0.0);
        double actualLng = (longitude != null) ? longitude : (lng != null ? lng : 0.0);
        List<BranchDistanceDto> list = branchService.findNearestBranches(actualLat, actualLng);
        if (latitude != null && longitude != null) {
            return ApiResponse.success(list.isEmpty() ? null : list.get(0));
        }
        return ApiResponse.success(list);
    }

    @GetMapping("/{code}")
    public ApiResponse<ClinicBranch> getBranchByCode(@PathVariable String code) {
        return ApiResponse.success(branchService.getBranchByCode(code));
    }

    @GetMapping("/{id}/directions")
    public ApiResponse<Object> getBranchDirections(
            @PathVariable Long id,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng) {
        // Return a mock directions URL
        return ApiResponse.success(java.util.Map.of("directionsUrl", "https://maps.google.com/?daddr=10.760624,106.587106"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<ClinicBranch> createBranch(@RequestBody ClinicBranch branch) {
        return ApiResponse.success("Tạo chi nhánh thành công", branchService.saveBranch(branch));
    }
}
