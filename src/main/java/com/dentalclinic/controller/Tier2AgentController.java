package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.Tier2AgentRequest;
import com.dentalclinic.dto.UpdateAgentStatusRequest;
import com.dentalclinic.model.AgentType;
import com.dentalclinic.model.Tier2Agent;
import com.dentalclinic.service.Tier2AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tier2-agents")
@CrossOrigin(origins = "*")
@Tag(name = "Tier-2 B2B Agents", description = "Quản lý đại lý cấp 2, phòng khám vệ tinh và nhà phân phối")
public class Tier2AgentController {

    private final Tier2AgentService agentService;

    public Tier2AgentController(Tier2AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Đăng ký đại lý cấp 2 / phòng khám vệ tinh mới")
    public ResponseEntity<ApiResponse<Tier2Agent>> registerAgent(@RequestBody Tier2AgentRequest req) {
        Tier2Agent created = agentService.registerAgent(req);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký đại lý cấp 2 thành công!", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DENTIST', 'RECEPTIONIST')")
    @Operation(summary = "Danh sách đại lý cấp 2 và đối tác phân phối")
    public ResponseEntity<ApiResponse<List<Tier2Agent>>> getAllAgents(
            @RequestParam(required = false) AgentType agentType) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAllAgents(agentType)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'DENTIST', 'RECEPTIONIST')")
    @Operation(summary = "Chi tiết đại lý cấp 2 theo ID")
    public ResponseEntity<ApiResponse<Tier2Agent>> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(agentService.getAgentById(id)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Cập nhật hạn mức công nợ và trạng thái đại lý")
    public ResponseEntity<ApiResponse<Tier2Agent>> updateAgentStatus(
            @PathVariable Long id,
            @RequestBody UpdateAgentStatusRequest req) {
        Tier2Agent updated = agentService.updateAgentStatus(id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đại lý thành công!", updated));
    }
}
