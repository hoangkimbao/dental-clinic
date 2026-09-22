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
    public ApiResponse<List<BranchDistanceDto>> getNearestBranches(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ApiResponse.success(branchService.findNearestBranches(lat, lng));
    }

    @GetMapping("/{code}")
    public ApiResponse<ClinicBranch> getBranchByCode(@PathVariable String code) {
        return ApiResponse.success(branchService.getBranchByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ApiResponse<ClinicBranch> createBranch(@RequestBody ClinicBranch branch) {
        return ApiResponse.success("Tạo chi nhánh thành công", branchService.saveBranch(branch));
    }
}
