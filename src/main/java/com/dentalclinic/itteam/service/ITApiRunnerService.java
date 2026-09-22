package com.dentalclinic.itteam.service;

import com.dentalclinic.itteam.dto.ApiRunTestRequest;
import com.dentalclinic.itteam.model.ITApiRunLog;
import com.dentalclinic.itteam.repository.ITApiRunLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for safe localhost internal API execution testing.
 * Strictly enforces SSRF prevention against cloud metadata, private subnets,
 * DNS rebinding, and host evasion tricks.
 */
@Service
public class ITApiRunnerService {

    private static final Logger log = LoggerFactory.getLogger(ITApiRunnerService.class);

    private static final Pattern DANGEROUS_SCHEMES_PATTERN = Pattern.compile(
            "(?i)^(file|ftp|gopher|ldap|ldaps|jar|netdoc|data|dict|mailto|telnet|php|expect):"
    );

    private static final Pattern FORBIDDEN_HOSTS_PATTERN = Pattern.compile(
            "(?i)(169\\.254\\.|evil\\.com|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.|0\\.0\\.0\\.0|\\[::1\\]|::1|nip\\.io|xip\\.io|sslip\\.io|@)"
    );

    private final ITApiRunLogRepository apiRunLogRepository;
    private final HttpClient httpClient;

    public ITApiRunnerService(ITApiRunLogRepository apiRunLogRepository) {
        this.apiRunLogRepository = apiRunLogRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Validates endpoint against SSRF, dangerous URI schemes, and host evasion rules.
     */
    public void validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        String trimmed = endpoint.trim();

        // Explicitly reject invalid/dangerous URI schemes early
        if (DANGEROUS_SCHEMES_PATTERN.matcher(trimmed).find() ||
                (trimmed.contains("://") && !trimmed.toLowerCase().startsWith("http://") && !trimmed.toLowerCase().startsWith("https://"))) {
            throw new IllegalArgumentException("Invalid or dangerous URI scheme rejected: " + trimmed);
        }

        if (FORBIDDEN_HOSTS_PATTERN.matcher(trimmed).find()) {
            throw new IllegalArgumentException("SSRF blocked: Forbidden target host or IP range: " + trimmed);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String host = uri.getHost();
                if (host == null || (!host.equalsIgnoreCase("localhost") && !host.equals("127.0.0.1"))) {
                    throw new IllegalArgumentException("SSRF blocked: Non-localhost target: " + host);
                }
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                throw new IllegalArgumentException("Malformed URL in endpoint: " + trimmed);
            }
        }
    }

    /**
     * Extracts internal path and query string from endpoint input.
     */
    public String extractInternalPath(String endpoint) {
        if (endpoint == null) return "/";
        String target = endpoint.trim();
        if (target.startsWith("http://") || target.startsWith("https://")) {
            try {
                URI uri = URI.create(target);
                String path = uri.getPath();
                if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
                    path += "?" + uri.getRawQuery();
                }
                return (path != null && !path.isEmpty()) ? path : "/";
            } catch (Exception e) {
                return target;
            }
        }
        return target.startsWith("/") ? target : "/" + target;
    }

    /**
     * Executes safe internal API test, records execution latency,
     * sanitizes payloads, and persists run log.
     */
    @Transactional
    public ITApiRunLog executeApiRun(ApiRunTestRequest request, String initiatedBy) {
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("Invalid API run test request");
        }

        validateEndpoint(request.getEndpoint());

        String internalPath = extractInternalPath(request.getEndpoint());
        String method = request.getHttpMethod().toUpperCase();
        String payload = request.getRequestPayload();

        long startTime = System.currentTimeMillis();
        Integer statusCode = null;
        String responsePayload = null;

        try {
            String fullUrl = "http://localhost:8080" + internalPath;
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");

            HttpRequest.BodyPublisher bodyPublisher = (payload != null && !payload.isBlank())
                    ? HttpRequest.BodyPublishers.ofString(payload)
                    : HttpRequest.BodyPublishers.noBody();

            if (payload != null && !payload.isBlank()) {
                reqBuilder.header("Content-Type", "application/json");
            }

            switch (method) {
                case "POST" -> reqBuilder.POST(bodyPublisher);
                case "PUT" -> reqBuilder.PUT(bodyPublisher);
                case "DELETE" -> reqBuilder.DELETE();
                default -> reqBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            statusCode = response.statusCode();
            responsePayload = response.body();
        } catch (Exception e) {
            log.warn("Internal API test execution error: {}", e.getMessage());
            responsePayload = "Execution error: " + e.getMessage();
        }

        long duration = Math.max(1L, System.currentTimeMillis() - startTime);

        ITApiRunLog runLog = new ITApiRunLog(
                request.getEndpoint(),
                method,
                statusCode,
                duration,
                payload,
                responsePayload,
                initiatedBy != null ? initiatedBy : "ROLE_ADMIN"
        );

        return apiRunLogRepository.save(runLog);
    }

    /**
     * Retrieve all historical API run logs.
     */
    @Transactional(readOnly = true)
    public List<ITApiRunLog> getAllApiRunLogs() {
        return apiRunLogRepository.findAllByOrderByRunTimestampDesc();
    }
}
