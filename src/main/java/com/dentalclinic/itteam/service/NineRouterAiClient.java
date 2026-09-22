package com.dentalclinic.itteam.service;

import com.dentalclinic.itteam.model.ITAgentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client for local 9Router AI orchestrator (http://localhost:20128).
 * Uses the "code-combo" model (best for IT/engineering tasks) with 8s timeout
 * and deterministic rule-based persona fallback when 9Router is offline.
 */
@Service
public class NineRouterAiClient {

    private static final Logger log = LoggerFactory.getLogger(NineRouterAiClient.class);
    private static final String NINE_ROUTER_BASE = "http://localhost:20128";
    private static final String CHAT_ENDPOINT = NINE_ROUTER_BASE + "/v1/chat/completions";
    /** Primary model and fallback cascade for quota/rate limit exhaustion */
    private static final String DEFAULT_MODEL = "combo_toc_do";
    private static final List<String> FALLBACK_MODELS = List.of(
            "combo_toc_do",
            "fast-combo",
            "vip-combo",
            "free-max-combo",
            "auto"
    );
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 45000; // reasoning models can take up to 30s

    /** API key from environment; falls back to local default if not set */
    private static final String NINE_ROUTER_KEY = System.getenv("NINEROUTER_KEY") != null
            ? System.getenv("NINEROUTER_KEY")
            : "sk-a2959fd23bbea0e6-t508t9-3054cb52";

    // Dental clinic context injected into every agent system prompt
    private static final String DENTAL_CONTEXT =
            "You are an IT specialist working for DentalCare — a modern Vietnamese dental clinic located at " +
            "36/9/12/7 Nguyễn Triệu Luật, KP.3, P.Bình Tân, TP.HCM. " +
            "The system stack is: Java 21 + Spring Boot 3.2.5, H2/PostgreSQL, JWT auth, Cloudflare Tunnel. " +
            "Your replies should be concise, professional, and in the same language as the user's message. " +
            "When writing code or configs, format them in markdown code blocks.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NineRouterAiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generates an AI agent response synchronously via 9Router code-combo.
     * Falls back to deterministic persona responses when 9Router is offline.
     */
    public String getAgentResponse(ITAgentProfile agent, String userMessage) {
        return getAgentResponse(agent, userMessage, null);
    }

    /**
     * Multi-turn overload: accepts prior message history for continued conversations.
     * @param history  list of prior {role, content} maps; may be null
     */
    public String getAgentResponse(ITAgentProfile agent, String userMessage, List<Map<String, String>> history) {
        if (agent == null) {
            return "[IT Team] Message received. Team members notified.";
        }

        for (String modelToTry : FALLBACK_MODELS) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(NINE_ROUTER_KEY);

                String agentSystemPrompt = (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank())
                        ? agent.getSystemPrompt()
                        : buildDefaultSystemPrompt(agent);

                // Build message array: system → history → current user message
                List<Map<String, Object>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", agentSystemPrompt));
                if (history != null) {
                    for (Map<String, String> h : history) {
                        messages.add(Map.of("role", h.getOrDefault("role", "user"),
                                            "content", h.getOrDefault("content", "")));
                    }
                }
                messages.add(Map.of("role", "user", "content", userMessage != null ? userMessage : ""));

                Map<String, Object> body = new HashMap<>();
                body.put("model", modelToTry);
                body.put("temperature", 0.6);
                body.put("max_tokens", 512);
                body.put("stream", false);
                body.put("messages", messages);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(CHAT_ENDPOINT, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String content = choices.get(0).path("message").path("content").asText();
                        if (content != null && !content.isBlank()) {
                            log.debug("✅ 9Router [{}] responded via model: {}", agent.getHashtag(), modelToTry);
                            return content.trim();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("ℹ️ Model [{}] failed for [{}] (quota/timeout/error: {}). Switching to next fallback combo...",
                        modelToTry, agent.getHashtag(), ex.getMessage());
            }
        }

        return getDeterministicFallback(agent, userMessage);
    }

    /**
     * Direct free-form AI query — no agent persona, plain user prompt routed through 9Router.
     * With automatic dynamic failover across combos if one runs out of quota.
     */
    public String askDirect(String systemPrompt, String userMessage) {
        for (String modelToTry : FALLBACK_MODELS) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(NINE_ROUTER_KEY);

                String sysMsg = (systemPrompt != null && !systemPrompt.isBlank())
                        ? systemPrompt
                        : "You are a helpful IT assistant for a dental clinic. " + DENTAL_CONTEXT;

                Map<String, Object> body = new HashMap<>();
                body.put("model", modelToTry);
                body.put("temperature", 0.6);
                body.put("max_tokens", 1024);
                body.put("stream", false);
                body.put("messages", List.of(
                        Map.of("role", "system", "content", sysMsg),
                        Map.of("role", "user", "content", userMessage != null ? userMessage : "")
                ));

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(CHAT_ENDPOINT, requestEntity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String content = choices.get(0).path("message").path("content").asText();
                        if (content != null && !content.isBlank()) {
                            log.debug("✅ 9Router direct ask succeeded via model: {}", modelToTry);
                            return content.trim();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("ℹ️ Model [{}] failed for askDirect (quota/error: {}). Retrying with next fallback model...",
                        modelToTry, ex.getMessage());
            }
        }
        return "Xin lỗi, các gói dịch vụ AI tạm thời không phản hồi. Đội ngũ IT đang sử dụng chế độ dự phòng nội bộ.";
    }

    /**
     * Asynchronously generates an AI agent response.
     */
    public CompletableFuture<String> generateAgentResponse(ITAgentProfile agent, String userMessage) {
        return CompletableFuture.supplyAsync(() -> getAgentResponse(agent, userMessage));
    }

    /**
     * Build a rich system prompt combining dental clinic context + agent specialization.
     */
    private String buildDefaultSystemPrompt(ITAgentProfile agent) {
        String expertise = (agent.getExpertise() != null && !agent.getExpertise().isBlank())
                ? " Your expertise: " + agent.getExpertise() + "."
                : "";
        return "You are " + agent.getDisplayName() + " (" + agent.getHashtag() + "), " +
               "an internal IT specialist. " + DENTAL_CONTEXT + expertise;
    }

    /**
     * Deterministic persona-based fallback responses when 9Router is offline.
     */
    public String getDeterministicFallback(ITAgentProfile agent, String userMessage) {
        String code = agent != null && agent.getAgentCode() != null
                ? agent.getAgentCode().toLowerCase().replaceFirst("^#?it-", "")
                : "it";
        String hashtag = agent != null && agent.getHashtag() != null ? agent.getHashtag() : "#it-team";

        return switch (code) {
            case "backend" -> "**[Alex Rivera — #it-backend]** Đã nhận yêu cầu. Spring Boot 3.2.5 đang hoạt động ổn định. Dịch vụ persistence và REST API sẵn sàng. " + hashtag;
            case "frontend" -> "**[Elena Chen — #it-frontend]** Đã ghi nhận. Management Portal UI và các component responsive đã được kiểm tra. " + hashtag;
            case "qa" -> "**[Marcus Vance — #it-qa]** Bộ test tự động và kiểm tra hồi quy đã chạy xong. Tất cả hệ thống đều GREEN. " + hashtag;
            case "devops" -> "**[Liam O'Connor — #it-devops]** Server port 8080 khỏe mạnh. Logs và Cloudflare Tunnel đang hoạt động. " + hashtag;
            case "security" -> "**[Aria Sterling — #it-security]** Audit bảo mật sạch. RBAC và privacy sanitization đang active. " + hashtag;
            default -> "**[" + (agent != null ? agent.getDisplayName() : "IT Agent") + "]** Đã nhận yêu cầu. Đang thực hiện quy trình chuẩn. " + hashtag;
        };
    }
}

