package com.dentalclinic.itteam.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.itteam.dto.*;
import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.ITAgentActivityRepository;
import com.dentalclinic.itteam.repository.ITAgentMemoryRepository;
import com.dentalclinic.itteam.repository.ITAgentProfileRepository;
import com.dentalclinic.itteam.repository.ITBrowserTabRecordRepository;
import com.dentalclinic.itteam.service.ITApiRunnerService;
import com.dentalclinic.itteam.service.ITMessagingService;
import com.dentalclinic.itteam.service.NineRouterAiClient;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for IT Team Command Center.
 * Exposes endpoints for managing agent profiles, long-term memories,
 * inter-agent messaging with hashtag routing, activity audit trail,
 * browser tab tracking, and safe localhost API execution.
 * Protected strictly by ROLE_ADMIN and ROLE_OWNER.
 */
@RestController
@RequestMapping("/api/it-team")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
@CrossOrigin(origins = "*")
@Tag(name = "10. IT Team Command Center", description = "Quản lý Đội ngũ IT nội bộ & Điều hành đa đặc vụ AI")
public class ITTeamController {

    private final ITAgentProfileRepository profileRepository;
    private final ITAgentMemoryRepository memoryRepository;
    private final ITAgentActivityRepository activityRepository;
    private final ITBrowserTabRecordRepository browserTabRepository;
    private final ITMessagingService messagingService;
    private final ITApiRunnerService apiRunnerService;
    private final UserRepository userRepository;
    private final NineRouterAiClient nineRouterAiClient;

    public ITTeamController(ITAgentProfileRepository profileRepository,
                            ITAgentMemoryRepository memoryRepository,
                            ITAgentActivityRepository activityRepository,
                            ITBrowserTabRecordRepository browserTabRepository,
                            ITMessagingService messagingService,
                            ITApiRunnerService apiRunnerService,
                            UserRepository userRepository,
                            NineRouterAiClient nineRouterAiClient) {
        this.profileRepository = profileRepository;
        this.memoryRepository = memoryRepository;
        this.activityRepository = activityRepository;
        this.browserTabRepository = browserTabRepository;
        this.messagingService = messagingService;
        this.apiRunnerService = apiRunnerService;
        this.userRepository = userRepository;
        this.nineRouterAiClient = nineRouterAiClient;
    }

    // =========================================================================
    // 1. Agent Profiles
    // =========================================================================

    @GetMapping("/agents")
    @Operation(summary = "Lấy danh sách hồ sơ 5 đặc vụ IT")
    public ResponseEntity<ApiResponse<List<ITAgentProfile>>> getAgents() {
        return ResponseEntity.ok(ApiResponse.success(profileRepository.findAll()));
    }

    @PutMapping("/agents/{id}/status")
    @Operation(summary = "Cập nhật trạng thái hoạt động của đặc vụ IT")
    public ResponseEntity<ApiResponse<ITAgentProfile>> updateAgentStatus(
            @PathVariable Long id,
            @RequestBody AgentStatusUpdateRequest request) {
        Optional<ITAgentProfile> profileOpt = profileRepository.findById(id);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy hồ sơ đặc vụ IT với ID: " + id));
        }

        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Trạng thái không hợp lệ. Cho phép: ONLINE, OFFLINE, BUSY, ACTIVE, AWAY"));
        }

        ITAgentProfile profile = profileOpt.get();
        String oldStatus = profile.getStatus();
        profile.setStatus(request.getStatus().toUpperCase());
        ITAgentProfile saved = profileRepository.save(profile);

        // Record audit activity
        activityRepository.save(new ITAgentActivity(
                saved.getId(),
                saved.getAgentCode(),
                "STATUS_UPDATE",
                "Trạng thái đặc vụ " + saved.getDisplayName() + " được cập nhật từ " + oldStatus + " sang " + saved.getStatus(),
                "Status: " + saved.getStatus(),
                "agent:" + saved.getId()
        ));

        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", saved));
    }

    // =========================================================================
    // 2. Persistent Memories
    // =========================================================================

    @GetMapping("/memories")
    @Operation(summary = "Lấy danh sách bộ nhớ dài hạn của đặc vụ IT")
    public ResponseEntity<ApiResponse<List<ITAgentMemory>>> getMemories(
            @RequestParam(required = false) String agentCode) {
        if (agentCode != null && !agentCode.isBlank()) {
            String clean = agentCode.trim();
            String alt = clean.startsWith("it-") ? clean.substring(3) : "it-" + clean;
            List<ITAgentMemory> list = memoryRepository.findByAgentCodeInOrderByLastUpdatedDesc(List.of(clean, alt));
            return ResponseEntity.ok(ApiResponse.success(list));
        }
        return ResponseEntity.ok(ApiResponse.success(memoryRepository.findAllByOrderByLastUpdatedDesc()));
    }

    @PostMapping("/memories")
    @Operation(summary = "Lưu hoặc cập nhật khóa bộ nhớ của đặc vụ IT")
    public ResponseEntity<ApiResponse<ITAgentMemory>> createOrUpdateMemory(
            @RequestBody CreateMemoryRequest request) {
        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Khóa bộ nhớ và nội dung không được để trống"));
        }

        String cleanCode = (request.getAgentCode() != null && !request.getAgentCode().isBlank())
                ? request.getAgentCode().trim()
                : "it-backend";
        String altCode = cleanCode.startsWith("it-") ? cleanCode.substring(3) : "it-" + cleanCode;

        Optional<ITAgentMemory> existingOpt = memoryRepository.findByAgentCodeAndMemoryKey(cleanCode, request.getMemoryKey());
        if (existingOpt.isEmpty()) {
            existingOpt = memoryRepository.findByAgentCodeAndMemoryKey(altCode, request.getMemoryKey());
        }

        ITAgentMemory memory;
        if (existingOpt.isPresent()) {
            memory = existingOpt.get();
            memory.setMemoryContent(request.getMemoryContent());
            memory.setPriority(request.getPriority());
            memory.setLastUpdated(LocalDateTime.now());
        } else {
            Long agentId = null;
            Optional<ITAgentProfile> profileOpt = profileRepository.findByAgentCode(cleanCode);
            if (profileOpt.isEmpty()) {
                profileOpt = profileRepository.findByAgentCode(altCode);
            }
            if (profileOpt.isPresent()) {
                agentId = profileOpt.get().getId();
            }
            memory = new ITAgentMemory(agentId, cleanCode, request.getMemoryKey(), request.getMemoryContent(), request.getPriority());
        }

        ITAgentMemory saved = memoryRepository.save(memory);

        activityRepository.save(new ITAgentActivity(
                saved.getAgentId(),
                saved.getAgentCode(),
                "MEMORY_UPDATE",
                "Lưu bộ nhớ cho đặc vụ " + saved.getAgentCode() + ": " + saved.getMemoryKey(),
                "Key: " + saved.getMemoryKey() + " (" + saved.getPriority() + ")",
                "memory:" + saved.getId()
        ));

        return ResponseEntity.ok(ApiResponse.success("Lưu bộ nhớ thành công", saved));
    }

    // =========================================================================
    // 3. Inter-Agent Messaging & Hashtag Engine
    // =========================================================================

    @GetMapping("/messages")
    @Operation(summary = "Lấy dòng tin nhắn hoặc hội thoại theo luồng (Thread)")
    public ResponseEntity<ApiResponse<List<ITAgentMessage>>> getMessages(
            @RequestParam(required = false) Long parentMessageId,
            @RequestParam(required = false) String hashtag) {
        List<ITAgentMessage> messages = messagingService.getMessages(parentMessageId, hashtag);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/messages")
    @Operation(summary = "Gửi tin nhắn điều hành, tự động bóc tách hashtag và định tuyến")
    public ResponseEntity<ApiResponse<ITAgentMessage>> dispatchMessage(
            @RequestBody DispatchMessageRequest request,
            Authentication authentication) {
        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Nội dung tin nhắn không được để trống hoặc chỉ chứa khoảng trắng"));
        }

        User senderUser = null;
        if (authentication != null && authentication.getName() != null) {
            senderUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        try {
            ITAgentMessage message = messagingService.dispatchMessage(request, senderUser);
            return ResponseEntity.ok(ApiResponse.success("Gửi tin nhắn thành công", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =========================================================================
    // 4. Activity Audit Trail
    // =========================================================================

    @GetMapping("/activities")
    @Operation(summary = "Xem nhật ký thao tác và kiểm toán hoạt động của đặc vụ")
    public ResponseEntity<?> getActivities(
            @RequestParam(required = false) String agentCode,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Chỉ số trang phải >= 0 và kích thước trang phải > 0"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ITAgentActivity> resultPage;

        boolean hasAgent = agentCode != null && !agentCode.isBlank();
        boolean hasAction = actionType != null && !actionType.isBlank();

        if (hasAgent && hasAction) {
            String clean = agentCode.trim();
            String alt = clean.startsWith("it-") ? clean.substring(3) : "it-" + clean;
            resultPage = activityRepository.findByAgentCodeInAndActionTypeOrderByTimestampDesc(List.of(clean, alt), actionType.trim(), pageable);
        } else if (hasAgent) {
            String clean = agentCode.trim();
            String alt = clean.startsWith("it-") ? clean.substring(3) : "it-" + clean;
            resultPage = activityRepository.findByAgentCodeInOrderByTimestampDesc(List.of(clean, alt), pageable);
        } else if (hasAction) {
            resultPage = activityRepository.findByActionTypeOrderByTimestampDesc(actionType.trim(), pageable);
        } else {
            resultPage = activityRepository.findAllByOrderByTimestampDesc(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(resultPage));
    }

    // =========================================================================
    // 5. Browser Tab Tracking
    // =========================================================================

    @GetMapping("/browser-tabs")
    @Operation(summary = "Lấy lịch sử các tab trình duyệt và phiên làm việc của đặc vụ")
    public ResponseEntity<ApiResponse<List<ITBrowserTabRecord>>> getBrowserTabs(
            @RequestParam(required = false) String agentCode) {
        List<ITBrowserTabRecord> tabs;
        if (agentCode != null && !agentCode.isBlank()) {
            String clean = agentCode.trim();
            String alt = clean.startsWith("it-") ? clean.substring(3) : "it-" + clean;
            tabs = browserTabRepository.findByAgentCodeInOrderByOpenedAtDesc(List.of(clean, alt));
        } else {
            tabs = browserTabRepository.findAllByOrderByOpenedAtDesc();
        }
        return ResponseEntity.ok(ApiResponse.success(tabs));
    }

    @PostMapping("/browser-tabs")
    @Operation(summary = "Ghi nhận phiên tab trình duyệt mở hoặc đóng")
    public ResponseEntity<ApiResponse<ITBrowserTabRecord>> recordBrowserTab(
            @RequestBody RecordBrowserTabRequest request) {
        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Đường dẫn tab (urlRoute) không được để trống"));
        }

        String agentCode = (request.getAgentCode() != null && !request.getAgentCode().isBlank())
                ? request.getAgentCode().trim()
                : "it-frontend";
        String altCode = agentCode.startsWith("it-") ? agentCode.substring(3) : "it-" + agentCode;

        Long agentId = null;
        Optional<ITAgentProfile> profileOpt = profileRepository.findByAgentCode(agentCode);
        if (profileOpt.isEmpty()) {
            profileOpt = profileRepository.findByAgentCode(altCode);
        }
        if (profileOpt.isPresent()) {
            agentId = profileOpt.get().getId();
        }

        ITBrowserTabRecord tab = new ITBrowserTabRecord(
                agentId,
                agentCode,
                request.getTabTitle(),
                request.getUrlRoute(),
                request.getTabCategory(),
                request.getStatus()
        );
        ITBrowserTabRecord saved = browserTabRepository.save(tab);

        activityRepository.save(new ITAgentActivity(
                agentId,
                agentCode,
                "TAB_" + saved.getStatus(),
                "Phiên tab " + saved.getStatus().toLowerCase() + ": " + (saved.getTabTitle() != null ? saved.getTabTitle() : saved.getUrlRoute()),
                saved.getUrlRoute(),
                "tab:" + saved.getId()
        ));

        return ResponseEntity.ok(ApiResponse.success("Ghi nhận tab thành công", saved));
    }

    // =========================================================================
    // 6. Safe Localhost API Runner & Monitor
    // =========================================================================

    @PostMapping("/api-runs")
    @Operation(summary = "Thực thi an toàn lệnh gọi API nội bộ localhost (Chống SSRF)")
    public ResponseEntity<ApiResponse<ITApiRunLog>> executeApiRun(
            @RequestBody ApiRunTestRequest request,
            Authentication authentication) {
        if (request == null || !request.isValid()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Endpoint API không được để trống"));
        }

        try {
            String initiatedBy = (authentication != null && authentication.getName() != null)
                    ? authentication.getName()
                    : "ROLE_ADMIN";
            ITApiRunLog runLog = apiRunnerService.executeApiRun(request, initiatedBy);
            return ResponseEntity.ok(ApiResponse.success("Thực thi API nội bộ thành công", runLog));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi thực thi API nội bộ: " + e.getMessage()));
        }
    }

    @GetMapping("/api-runs")
    @Operation(summary = "Lấy lịch sử các lần chạy kiểm thử API nội bộ")
    public ResponseEntity<ApiResponse<List<ITApiRunLog>>> getApiRuns() {
        return ResponseEntity.ok(ApiResponse.success(apiRunnerService.getAllApiRunLogs()));
    }

    // =========================================================================
    // 6. 9Router AI Ask Endpoints
    // =========================================================================

    /**
     * POST /api/it-team/ask
     * Direct AI query routed through 9Router code-combo.
     * Body: { "question": "...", "systemPrompt": "..." (optional) }
     */
    @PostMapping("/ask")
    @Operation(summary = "Hỏi AI trực tiếp qua 9Router (code-combo)")
    public ResponseEntity<ApiResponse<Map<String, String>>> askAi(
            @RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "").trim();
        if (question.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Câu hỏi không được để trống"));
        }
        String systemPrompt = body.get("systemPrompt");
        String answer = nineRouterAiClient.askDirect(systemPrompt, question);
        return ResponseEntity.ok(ApiResponse.success("OK", Map.of("answer", answer, "model", "combo_toc_do")));
    }

    /**
     * POST /api/it-team/ask/agent/{agentId}
     * Ask a specific IT agent (with its persona) via 9Router.
     * Body: { "message": "..." }
     */
    @PostMapping("/ask/agent/{agentId}")
    @Operation(summary = "Hỏi một đặc vụ IT cụ thể qua 9Router với vai diễn nhân vật")
    public ResponseEntity<ApiResponse<Map<String, String>>> askAgent(
            @PathVariable Long agentId,
            @RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tin nhắn không được để trống"));
        }
        return profileRepository.findById(agentId)
                .map(agent -> {
                    String reply = nineRouterAiClient.getAgentResponse(agent, message);
                    return ResponseEntity.ok(ApiResponse.success("OK",
                            Map.of("reply", reply, "agent", agent.getHashtag(), "model", "combo_toc_do")));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
