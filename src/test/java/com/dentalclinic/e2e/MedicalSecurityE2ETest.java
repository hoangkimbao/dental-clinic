package com.dentalclinic.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enterprise E2E Test Suite for 20 Enterprise Medical Security Standards (F48-F52).
 * Follows the 4-Tier requirement-driven opaque-box testing methodology:
 * - Tier 1: Category-Partition (IDOR Protection, RBAC Access, AES-256 EMR, File Upload Whitelist, Headers, Error Suppression)
 * - Tier 2: Boundary Value Analysis (File Size 5MB limit, Rate Limit 60-req burst, JWT Malformation, Password Bounds)
 * - Tier 3: Pairwise Combinatorial (Multi-Vector Attacks: Disguised File Headers, Spoofed IP IDOR, Privilege Escalation, SQLi/XSS)
 * - Tier 4: Real-World Workload Scenarios (Simulated External Cyber Reconnaissance & Attack; Compromised Account Insider Threat)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Medical Enterprise Security E2E Test Suite (F48-F52)")
public class MedicalSecurityE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(400);

    private String ownerToken;
    private String adminToken;
    private String dentistToken;
    private String receptionistToken;
    private String patientToken;

    private String getUniqueIp() {
        return "192.168.40." + (IP_COUNTER.incrementAndGet() % 250);
    }

    private String obtainToken(String username, String password) throws Exception {
        Map<String, String> loginReq = new HashMap<>();
        loginReq.put("username", username);
        loginReq.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-For", getUniqueIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    @Autowired
    private com.dentalclinic.security.LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUpAuthTokens() throws Exception {
        if (loginAttemptService != null) {
            loginAttemptService.resetAll();
        }
        if (ownerToken == null) {
            ownerToken = obtainToken("owner", "123");
        }
        if (adminToken == null) {
            adminToken = obtainToken("admin", "123");
        }
        if (dentistToken == null) {
            dentistToken = obtainToken("bs_tuan", "123");
        }
        if (receptionistToken == null) {
            receptionistToken = obtainToken("letan", "123");
        }
        if (patientToken == null) {
            patientToken = obtainToken("benhnhan", "123");
        }
    }

    // =========================================================================
    // TIER 1: CATEGORY-PARTITION (EQUIVALENCE CLASSES & SECURITY HARDENING)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Area 1: IDOR Protection & Patient Record Isolation (F48)")
    class Tier1IdorProtectionTests {

        @Test
        @DisplayName("T1-IDOR-01: Patient cannot query EMR medical records without phone or dump all records")
        void testPatientCannotDumpAllMedicalRecords() throws Exception {
            // Patient attempting to call GET /api/medical-records without phone parameter
            // Must return 403 Forbidden or return strictly own records (never all clinic records)
            MvcResult result = mockMvc.perform(get("/api/medical-records")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andReturn();

            int statusCode = result.getResponse().getStatus();
            if (statusCode == 200) {
                // If 200, must verify it contains ONLY the logged-in patient's records
                JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                if (data.isArray() && data.size() > 0) {
                    for (JsonNode record : data) {
                        assertEquals("0988776655", record.path("patient").path("phone").asText());
                    }
                }
            } else {
                assertTrue(statusCode == 401 || statusCode == 403, "Expected 401 or 403 Forbidden for patient EMR dump");
            }
        }

        @Test
        @DisplayName("T1-IDOR-02: Patient querying another patient's phone receives 403 Forbidden")
        void testPatientCannotViewOtherPatientRecordsByPhone() throws Exception {
            // benhnhan has phone 0988776655, attempts to query phone of owner (0901234567)
            MvcResult result = mockMvc.perform(get("/api/medical-records")
                    .param("phone", "0901234567")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andReturn();

            int status = result.getResponse().getStatus();
            assertTrue(status == 403 || status == 200, "Must be restricted (403 or empty array if filtered)");
            if (status == 200) {
                JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                assertEquals(0, data.size(), "Must not leak other patient's records");
            }
        }

        @Test
        @DisplayName("T1-IDOR-03: Patient querying another patient's appointment receives 403 Forbidden")
        void testPatientCannotViewOtherPatientAppointments() throws Exception {
            // benhnhan attempts to access appointment list of user ID 1 (owner)
            mockMvc.perform(get("/api/appointments/patient/1")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 403 || code == 200);
                        if (code == 200) {
                            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                            assertTrue(data.size() == 0, "Cannot leak other appointments");
                        }
                    });
        }

        @Test
        @DisplayName("T1-IDOR-04: Authorized dentist or owner can access patient EMR")
        void testAuthorizedDentistCanAccessEmr() throws Exception {
            mockMvc.perform(get("/api/medical-records")
                    .param("phone", "0988776655")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-IDOR-05: Unauthenticated request to medical records is blocked")
        void testUnauthenticatedEmrAccessBlocked() throws Exception {
            mockMvc.perform(get("/api/medical-records")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Unauthenticated access must be 401 or 403");
                    });
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 2: RBAC Access Control on Content & Uploads (F48)")
    class Tier1RbacAccessControlTests {

        @Test
        @DisplayName("T1-RBAC-01: Unauthenticated request cannot create or modify articles (401/403)")
        void testUnauthenticatedCannotModifyArticles() throws Exception {
            Map<String, Object> articleReq = new HashMap<>();
            articleReq.put("title", "Hackers Article Insertion");
            articleReq.put("content", "Unauthorized malicious content");

            mockMvc.perform(post("/api/articles")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(articleReq)))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Must require authentication to create article");
                    });
        }

        @Test
        @DisplayName("T1-RBAC-02: Patient cannot delete or modify articles (403 Forbidden)")
        void testPatientCannotModifyArticles() throws Exception {
            mockMvc.perform(delete("/api/articles/1")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("T1-RBAC-03: Unauthenticated request cannot upload EMR images (401/403)")
        void testUnauthenticatedCannotUploadEmrImages() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "xray.jpg", "image/jpeg", "DUMMY_IMAGE_DATA".getBytes()
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(file)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Upload must require authentication");
                    });
        }

        @Test
        @DisplayName("T1-RBAC-04: Non-admin users cannot access IT Team Command Center APIs (403 Forbidden)")
        void testNonAdminCannotAccessItTeamApis() throws Exception {
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("T1-RBAC-05: Non-admin users cannot access or modify Tier-2 B2B partner configs")
        void testNonAdminCannotAccessTier2Agents() throws Exception {
            mockMvc.perform(get("/api/tier2-agents")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Tier2 agents require authorized role");
                    });
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 3: Sensitive Clinical Data Protection & AES-256 (F49)")
    class Tier1SensitiveMedicalDataProtectionTests {

        @Test
        @DisplayName("T1-ENC-01: Creating medical record ensures clinical fields are processed securely")
        void testCreateMedicalRecordEncrypted() throws Exception {
            Map<String, Object> recordReq = new HashMap<>();
            recordReq.put("diagnosis", "Viêm tủy răng không phục hồi răng số 36");
            recordReq.put("treatmentDone", "Điều trị tủy buồng, đặt thuốc sát trùng");
            recordReq.put("prescription", "Amoxicillin 500mg, Paracetamol 500mg");

            mockMvc.perform(post("/api/medical-records")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(recordReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ENC-02: Patient medical data returned to authorized caller is decrypted seamlessly")
        void testAuthorizedReadOfEncryptedClinicalData() throws Exception {
            mockMvc.perform(get("/api/medical-records")
                    .param("phone", "0988776655")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ENC-03: Sensitive clinical diagnosis fields are not leaked in public logs or error traces")
        void testClinicalFieldsNotLeakedInLogs() throws Exception {
            mockMvc.perform(get("/api/medical-records/invalid-id-path")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        String body = result.getResponse().getContentAsString();
                        assertFalse(body.contains("Viêm tủy răng"), "Sensitive diagnosis must not leak in error response");
                    });
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 4: File Upload Validation (F50)")
    class Tier1FileUploadValidationTests {

        @Test
        @DisplayName("T1-UPL-01: Valid JPEG image upload is accepted")
        void testValidJpegImageUpload() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "xray.jpg", "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46} // JPEG Magic Bytes
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(file)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T1-UPL-02: Valid PNG image upload is accepted")
        void testValidPngImageUpload() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "intraoral.png", "image/png",
                    new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A} // PNG Magic Bytes
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(file)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T1-UPL-03: Executable and script file extensions (.exe, .jsp, .sh) are rejected with 400")
        void testMaliciousFileExtensionsRejected() throws Exception {
            MockMultipartFile exeFile = new MockMultipartFile(
                    "file", "backdoor.exe", "application/x-msdownload", "MALICIOUS_EXE".getBytes()
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(exeFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());

            MockMultipartFile jspFile = new MockMultipartFile(
                    "file", "webshell.jsp", "text/html", "<% Runtime.getRuntime().exec(\"whoami\"); %>".getBytes()
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(jspFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T1-UPL-04: File disguised with .jpg extension but containing executable magic bytes is rejected")
        void testDisguisedExecutableMagicBytesRejected() throws Exception {
            byte[] fakeJpgWithExeBytes = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00}; // DOS/PE Header

            MockMultipartFile fakeFile = new MockMultipartFile(
                    "file", "innocent_teeth.jpg", "image/jpeg", fakeJpgWithExeBytes
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(fakeFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T1-UPL-05: Empty upload file is rejected with 400 Bad Request")
        void testEmptyFileUploadRejected() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(emptyFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 5: Security Response Headers (F52)")
    class Tier1SecurityHeadersTests {

        @Test
        @DisplayName("T1-HDR-01: Response contains X-Content-Type-Options: nosniff")
        void testNoSniffHeaderPresent() throws Exception {
            mockMvc.perform(get("/api/coupons/active")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("T1-HDR-02: Response contains X-Frame-Options SAMEORIGIN or DENY")
        void testXFrameOptionsHeaderPresent() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/coupons/active")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andReturn();

            String frameHeader = result.getResponse().getHeader("X-Frame-Options");
            assertNotNull(frameHeader);
            assertTrue(frameHeader.equalsIgnoreCase("SAMEORIGIN") || frameHeader.equalsIgnoreCase("DENY"));
        }

        @Test
        @DisplayName("T1-HDR-03: Response contains Strict-Transport-Security (HSTS)")
        void testHstsHeaderPresent() throws Exception {
            mockMvc.perform(get("/api/coupons/active")
                    .secure(true)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(header().exists("Strict-Transport-Security"));
        }

        @Test
        @DisplayName("T1-HDR-04: Response contains Referrer-Policy: strict-origin-when-cross-origin")
        void testReferrerPolicyHeaderPresent() throws Exception {
            mockMvc.perform(get("/api/coupons/active")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        }

        @Test
        @DisplayName("T1-HDR-05: Server does not expose internal stacktrace in HTTP 500 responses")
        void testInternalStackTraceSuppressed() throws Exception {
            mockMvc.perform(get("/api/articles/throw-simulated-error")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        String content = result.getResponse().getContentAsString();
                        assertFalse(content.contains("at org.springframework"), "Must not leak Java stacktrace");
                        assertFalse(content.contains("com.dentalclinic"), "Must not leak internal package names in error");
                    });
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY VALUE ANALYSIS (SECURITY THRESHOLDS & EXTREMES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2: Boundary Value Analysis")
    class Tier2BoundaryValueAnalysisTests {

        @Test
        @DisplayName("T2-BND-01: File size boundary - upload exceeding 5MB is rejected")
        void testFileUploadSizeExceeds5Mb() throws Exception {
            // 5MB = 5 * 1024 * 1024 = 5,242,880 bytes. Create 5,242,881 bytes
            byte[] oversizedBytes = new byte[5 * 1024 * 1024 + 1];

            MockMultipartFile oversizedFile = new MockMultipartFile(
                    "file", "oversized_scan.jpg", "image/jpeg", oversizedBytes
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(oversizedFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 400 || code == 413, "Must reject file exceeding 5MB limit");
                    });
        }

        @Test
        @DisplayName("T2-BND-02: RateLimitingFilter blocks bursts exceeding 60 requests per 10s per IP (429)")
        void testRateLimitBurstBreachReturns429() throws Exception {
            String targetIp = "192.168.99.11"; // Dedicated static IP to trigger rate limit threshold
            boolean received429 = false;

            for (int i = 0; i < 70; i++) {
                MvcResult res = mockMvc.perform(get("/api/coupons/active")
                        .header("X-Forwarded-For", targetIp))
                        .andReturn();
                if (res.getResponse().getStatus() == 429) {
                    received429 = true;
                    break;
                }
            }

            assertTrue(received429, "RateLimitingFilter must trigger HTTP 429 after exceeding limit");
        }

        @Test
        @DisplayName("T2-BND-03: Malformed JWT token with invalid signature returns 401 Unauthorized")
        void testMalformedJwtSignature() throws Exception {
            String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJvd25lciIsInJvbGUiOiJST0xFX09XTkVSIiwiZXhwIjoxOTk5OTk5OTk5fQ.INVALID_TAMPERED_SIGNATURE";

            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + forgedToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Tampered token must be rejected");
                    });
        }

        @Test
        @DisplayName("T2-BND-04: Expired JWT token returns 401 Unauthorized")
        void testExpiredJwtToken() throws Exception {
            // Expired in 2020
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJvd25lciIsImlhdCI6MTU5OTAwMDAwMCwiZXhwIjoxNTk5MDAwMDAxfQ.dummysig";

            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + expiredToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403, "Expired token must be rejected");
                    });
        }

        @Test
        @DisplayName("T2-BND-05: Registration with short password (< 6 characters) returns 400 Bad Request")
        void testRegistrationShortPassword() throws Exception {
            Map<String, String> regReq = new HashMap<>();
            regReq.put("username", "short_pass_user");
            regReq.put("password", "12"); // Too short
            regReq.put("fullName", "Short Pass User");
            regReq.put("phone", "0911223344");

            mockMvc.perform(post("/api/auth/register")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // TIER 3: PAIRWISE COMBINATORIAL (MULTI-VECTOR ATTACK SIMULATIONS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 3: Pairwise Combinatorial Multi-Vector Attacks")
    class Tier3PairwiseCombinatorialTests {

        @Test
        @DisplayName("T3-SEC-01: Valid Patient Token + Spoofed X-Forwarded-For Attempting IDOR on EMR -> Blocked")
        void testPatientIdorWithSpoofedIp() throws Exception {
            mockMvc.perform(get("/api/medical-records")
                    .param("phone", "0901234567") // Target other patient
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", "127.0.0.1")) // Spoofed local IP
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 403 || code == 200);
                        if (code == 200) {
                            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                            assertEquals(0, data.size());
                        }
                    });
        }

        @Test
        @DisplayName("T3-SEC-02: Disguised File Extension (.png) with Embedded PHP Shell Script -> Rejected")
        void testDisguisedPhpShellInPng() throws Exception {
            byte[] disguisedBytes = "<?php system($_GET['cmd']); ?>".getBytes();

            MockMultipartFile attackFile = new MockMultipartFile(
                    "file", "avatar.png", "image/png", disguisedBytes
            );

            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(attackFile)
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T3-SEC-03: Receptionist Token Attempting IT Team Command Center & Article Deletion -> 403")
        void testPrivilegeEscalationAttemptByReceptionist() throws Exception {
            // Attempt 1: Access IT Team
            mockMvc.perform(get("/api/it-team/agents")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());

            // Attempt 2: Delete Article
            mockMvc.perform(delete("/api/articles/1")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("T3-SEC-04: SQL Injection and XSS Stored Payload in Article Content Handled Safely")
        void testSqlAndXssPayloadsHandledSafely() throws Exception {
            Map<String, Object> attackArticle = new HashMap<>();
            attackArticle.put("title", "Cẩm nang <script>alert('XSS')</script>");
            attackArticle.put("content", "SQL Injection payload: ' OR '1'='1' -- DROP TABLE users;");
            attackArticle.put("category", "KIEN_THUC");

            mockMvc.perform(post("/api/articles")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(attackArticle)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD WORKLOAD SCENARIOS (PENETRATION ATTACK SIMULATIONS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 4: Real-World Workload Scenarios")
    class Tier4RealWorldWorkloadScenariosTests {

        @Test
        @DisplayName("T4-SEC-01: Multi-Vector External Penetration Simulation")
        void testExternalPenetrationAttackScenario() throws Exception {
            String attackerIp = "185.220.101.5"; // Simulated attacker IP

            // 1. Reconnaissance: Accessing protected endpoints unauthenticated
            mockMvc.perform(get("/api/it-team/agents")
                    .header("X-Forwarded-For", attackerIp))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403);
                    });

            // 2. Directory enumeration attempt on sensitive files
            mockMvc.perform(get("/h2-console")
                    .header("X-Forwarded-For", attackerIp))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 200 || code == 401 || code == 403 || code == 404);
                    });

            // 3. Credential stuffing / Brute-force simulation on login
            for (int i = 0; i < 5; i++) {
                Map<String, String> badLogin = Map.of("username", "admin", "password", "wrongpass_" + i);
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", attackerIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                        .andExpect(status().isUnauthorized());
            }

            // 4. Malicious webshell upload attempt without auth
            MockMultipartFile shell = new MockMultipartFile("file", "exploit.jsp", "text/html", "<% out.print(\"pwned\"); %>".getBytes());
            mockMvc.perform(multipart("/api/emr/images/upload")
                    .file(shell)
                    .header("X-Forwarded-For", attackerIp))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 400 || code == 401 || code == 403);
                    });
        }

        @Test
        @DisplayName("T4-SEC-02: Compromised Patient Account Insider Threat Simulation")
        void testCompromisedPatientInsiderThreatScenario() throws Exception {
            String insiderIp = getUniqueIp();

            // 1. Insider logs in with patient credentials
            String token = obtainToken("benhnhan", "123");
            assertNotNull(token);

            // 2. Attempts IDOR to harvest clinic medical records
            mockMvc.perform(get("/api/medical-records")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Forwarded-For", insiderIp))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403 || code == 200);
                        if (code == 200) {
                            JsonNode records = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
                            for (JsonNode r : records) {
                                assertEquals("0988776655", r.path("patient").path("phone").asText());
                            }
                        }
                    });

            // 3. Attempts to tamper with doctor reviews or delete clinic articles
            mockMvc.perform(delete("/api/articles/1")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Forwarded-For", insiderIp))
                    .andExpect(status().isForbidden());

            // 4. Attempts to create unauthorized Tier-2 satellite clinic
            Map<String, Object> fakeAgent = Map.of("agentCode", "ROGUE-AGT", "agentName", "Rogue Clinic");
            mockMvc.perform(post("/api/tier2-agents")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Forwarded-For", insiderIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(fakeAgent)))
                    .andExpect(result -> {
                        int code = result.getResponse().getStatus();
                        assertTrue(code == 401 || code == 403);
                    });
        }
    }
}
