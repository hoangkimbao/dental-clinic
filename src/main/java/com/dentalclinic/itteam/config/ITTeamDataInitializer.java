package com.dentalclinic.itteam.config;

import com.dentalclinic.itteam.model.*;
import com.dentalclinic.itteam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data seeder for IT Team Command Center.
 * Automatically initializes the 5 standard IT agent profiles, initial memories,
 * virtual browser tabs, and initial bootstrap activities on startup.
 * Checks itAgentProfileRepository.count() == 0 for idempotency across restarts.
 */
@Component
@Order(2)
public class ITTeamDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ITTeamDataInitializer.class);

    private final ITAgentProfileRepository profileRepository;
    private final ITAgentMemoryRepository memoryRepository;
    private final ITAgentMessageRepository messageRepository;
    private final ITAgentActivityRepository activityRepository;
    private final ITBrowserTabRecordRepository tabRepository;
    private final ITApiRunLogRepository apiRunLogRepository;

    public ITTeamDataInitializer(
            ITAgentProfileRepository profileRepository,
            ITAgentMemoryRepository memoryRepository,
            ITAgentMessageRepository messageRepository,
            ITAgentActivityRepository activityRepository,
            ITBrowserTabRecordRepository tabRepository,
            ITApiRunLogRepository apiRunLogRepository) {
        this.profileRepository = profileRepository;
        this.memoryRepository = memoryRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.tabRepository = tabRepository;
        this.apiRunLogRepository = apiRunLogRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🤖 Checking IT Team Command Center initial data...");

        seedStandardAgentProfiles();
        seedAgentMemories();
        seedBrowserTabs();
        seedInitialMessages();
        seedInitialActivities();

        log.info("✅ IT Team Command Center data check and initialization complete.");
    }

    private void seedStandardAgentProfiles() {
        List<ITAgentProfile> standardProfiles = List.of(
                new ITAgentProfile(
                        "backend",
                        "#it-backend",
                        "Alex Rivera (Backend Architect)",
                        "Backend Architect",
                        "ONLINE",
                        "Spring Boot, Database, Security, REST APIs",
                        "fa-server",
                        "You are Alex Rivera, Senior Backend Architect for DentalCare Management Portal. You specialize in Spring Boot 3.2.5, Spring Data JPA, REST API design, and robust transaction management. Always provide clean, production-ready Java code."
                ),
                new ITAgentProfile(
                        "frontend",
                        "#it-frontend",
                        "Elena Chen (Frontend Lead)",
                        "Management Portal UI Specialist",
                        "ONLINE",
                        "Management Portal UI, API Client, Responsive UX",
                        "fa-desktop",
                        "You are Elena Chen, Frontend Lead for DentalCare Management Portal. You are an expert in vanilla ES6+, Tailwind CSS, responsive healthcare dashboards, and dynamic UI interactions."
                ),
                new ITAgentProfile(
                        "qa",
                        "#it-qa",
                        "Marcus Vance (QA & Test Automation)",
                        "QA & Reliability Engineer",
                        "ONLINE",
                        "API Testing, Authorization Checks, Regression",
                        "fa-vial-circle-check",
                        "You are Marcus Vance, QA & Test Automation Specialist. You focus on automated API testing, regression validation, RBAC verification, and edge case detection across all DentalCare services."
                ),
                new ITAgentProfile(
                        "devops",
                        "#it-devops",
                        "Liam O'Connor (DevOps & SRE)",
                        "DevOps & Infrastructure Engineer",
                        "ONLINE",
                        "Build, Server Runtime, Logs, Tunnel Integration",
                        "fa-network-wired",
                        "You are Liam O'Connor, DevOps & SRE for DentalCare. You oversee Maven builds, H2/PostgreSQL runtime operations, application logs, and local proxy/tunnel routing."
                ),
                new ITAgentProfile(
                        "security",
                        "#it-security",
                        "Aria Sterling (Security Specialist)",
                        "Cybersecurity & Compliance Officer",
                        "ONLINE",
                        "RBAC, Data Privacy, Input Validation, Audit Logs",
                        "fa-shield-halved",
                        "You are Aria Sterling, Lead Security Architect. You strictly enforce HIPAA/GDPR healthcare privacy, JWT token security, role-based access control (RBAC), and prevent PII leakage."
                )
        );

        for (ITAgentProfile profile : standardProfiles) {
            if (!profileRepository.existsByAgentCode(profile.getAgentCode())) {
                profileRepository.save(profile);
                log.info("   -> Seeded agent profile: {} ({})", profile.getDisplayName(), profile.getHashtag());
            }
        }
    }

    private void seedAgentMemories() {
        record MemorySeed(String agentCode, String key, String content, String priority) {}

        List<MemorySeed> seeds = List.of(
                // #it-backend
                new MemorySeed(
                        "backend",
                        "ARCHITECTURE_OVERVIEW",
                        "DentalCare core runs Spring Boot 3.2.5 on Java 17 with Spring Data JPA and H2/PostgreSQL database.",
                        "HIGH"
                ),
                new MemorySeed(
                        "backend",
                        "PERSISTENCE_LAYER",
                        "Entities extend com.dentalclinic.common.BaseEntity for automatic createdAt and updatedAt auditing.",
                        "MEDIUM"
                ),
                new MemorySeed(
                        "backend",
                        "REST_API_CONVENTION",
                        "All controller endpoints return ApiResponse<T> envelope with standard success, message, data, and timestamp.",
                        "MEDIUM"
                ),

                // #it-frontend
                new MemorySeed(
                        "frontend",
                        "UI_STACK",
                        "Portal frontend uses vanilla ES6, Tailwind CSS, FontAwesome icons, and static SPA routing.",
                        "HIGH"
                ),
                new MemorySeed(
                        "frontend",
                        "CLIENT_AUTH",
                        "JWT token stored in sessionStorage('token'); sent in Authorization Bearer header.",
                        "HIGH"
                ),
                new MemorySeed(
                        "frontend",
                        "PORTAL_NAVIGATION",
                        "IT Team Command Center lives in #section-itteam with 5 tab views: Profiles, Chat, Memories, Logs, API Monitor.",
                        "MEDIUM"
                ),

                // #it-qa
                new MemorySeed(
                        "qa",
                        "TEST_SUITE_STATUS",
                        "Automated test suite covers auth endpoints, appointment booking, and role permissions.",
                        "HIGH"
                ),
                new MemorySeed(
                        "qa",
                        "REGRESSION_CHECKLIST",
                        "Verify 401/403 responses for unauthenticated requests and check non-interference with booking flow.",
                        "HIGH"
                ),

                // #it-devops
                new MemorySeed(
                        "devops",
                        "SERVER_RUNTIME",
                        "Application runs on port 8080. Local 9Router AI orchestrator accessible on port 20128.",
                        "HIGH"
                ),
                new MemorySeed(
                        "devops",
                        "DATABASE_STORAGE",
                        "H2 database file stored at ./data/dentaldb.mv.db with AUTO_SERVER=TRUE.",
                        "MEDIUM"
                ),

                // #it-security
                new MemorySeed(
                        "security",
                        "PRIVACY_GUARDRAIL",
                        "Strictly redact JWT tokens, passwords, Authorization headers, and EMR medical records from all logs.",
                        "HIGH"
                ),
                new MemorySeed(
                        "security",
                        "RBAC_POLICY",
                        "IT Team endpoints at /api/it-team/** strictly require ROLE_ADMIN or ROLE_OWNER authority.",
                        "HIGH"
                )
        );

        for (MemorySeed s : seeds) {
            if (!memoryRepository.existsByAgentCodeAndMemoryKey(s.agentCode(), s.key())) {
                ITAgentProfile profile = profileRepository.findByAgentCode(s.agentCode()).orElse(null);
                Long agentId = profile != null ? profile.getId() : null;

                ITAgentMemory mem = new ITAgentMemory(
                        agentId,
                        s.agentCode(),
                        s.key(),
                        s.content(),
                        s.priority()
                );
                memoryRepository.save(mem);
            }
        }
    }

    private void seedBrowserTabs() {
        record TabSeed(String agentCode, String title, String url, String category, String status) {}

        List<TabSeed> initialTabs = List.of(
                new TabSeed(
                        "frontend",
                        "DentalCare Management Portal",
                        "http://localhost:8080/index.html",
                        "PORTAL",
                        "OPEN"
                ),
                new TabSeed(
                        "backend",
                        "OpenAPI 3.0 / Swagger UI",
                        "http://localhost:8080/swagger-ui/index.html",
                        "DOCS",
                        "BACKGROUND"
                ),
                new TabSeed(
                        "devops",
                        "H2 Database Console",
                        "http://localhost:8080/h2-console",
                        "MONITORING",
                        "BACKGROUND"
                ),
                new TabSeed(
                        "security",
                        "IT Team Security Audit & RBAC Monitor",
                        "http://localhost:8080/api/it-team/activities",
                        "API_TESTER",
                        "OPEN"
                ),
                new TabSeed(
                        "qa",
                        "API Runner & Endpoint Inspector",
                        "http://localhost:8080/api/it-team/api-runs",
                        "API_TESTER",
                        "BACKGROUND"
                )
        );

        for (TabSeed t : initialTabs) {
            if (!tabRepository.existsByAgentCodeAndUrlRoute(t.agentCode(), t.url())) {
                ITAgentProfile profile = profileRepository.findByAgentCode(t.agentCode()).orElse(null);
                Long agentId = profile != null ? profile.getId() : null;

                ITBrowserTabRecord record = new ITBrowserTabRecord(
                        agentId,
                        t.agentCode(),
                        t.title(),
                        t.url(),
                        t.category(),
                        t.status()
                );
                tabRepository.save(record);
            }
        }
    }

    private void seedInitialMessages() {
        if (!messageRepository.existsByRecipientHashtag("BROADCAST")) {
            ITAgentMessage welcomeMessage = new ITAgentMessage(
                    1L,
                    "BS.CKII Trần Văn Thắng (Chủ Phòng Khám)",
                    "USER",
                    null,
                    "BROADCAST",
                    "Welcome team to the DentalCare IT Command Center! Please report your current operational status. #it-backend #it-frontend #it-qa #it-devops #it-security",
                    "#it-backend,#it-frontend,#it-qa,#it-devops,#it-security",
                    null
            );
            messageRepository.save(welcomeMessage);
            log.info("   -> Seeded initial IT team broadcast welcome message.");
        }
    }

    private void seedInitialActivities() {
        if (!activityRepository.existsByAgentCodeAndActionType("devops", "SYSTEM_BOOT")) {
            ITAgentProfile devops = profileRepository.findByAgentCode("devops").orElse(null);
            Long devopsId = devops != null ? devops.getId() : null;

            ITAgentActivity bootActivity = new ITAgentActivity(
                    devopsId,
                    "devops",
                    "SYSTEM_BOOT",
                    "IT Team Command Center services initialized successfully.",
                    "Seeded 5 IT agent profiles, initial memories, and browser tab records.",
                    "/api/it-team/agents"
            );
            activityRepository.save(bootActivity);
            log.info("   -> Seeded initial system boot activity record.");
        }
    }
}
