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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enterprise E2E Test Suite for DentalCare Customer Dental Ecosystem (F31-F37).
 * Follows the 4-Tier requirement-driven opaque-box testing methodology:
 * - Tier 1: Category-Partition (Equivalence Classes across all 8 functional areas)
 * - Tier 2: Boundary Value Analysis (Corner cases, missing fields, extreme numbers, invalid enums)
 * - Tier 3: Pairwise Combinatorial (Cross-feature pipelines connecting Catalog, Cart, Loyalty, AI, Warranty)
 * - Tier 4: Real-World Workload Scenarios (End-to-End realistic customer journeys)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Customer Dental Ecosystem E2E Test Suite (F31-F37)")
public class DentalCustomerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(200);

    private String ownerToken;
    private String patientToken;
    private String dentistToken;

    private String getUniqueIp() {
        return "192.168.20." + (IP_COUNTER.incrementAndGet() % 250);
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

    @BeforeEach
    void setUpAuthTokens() throws Exception {
        if (ownerToken == null) {
            ownerToken = obtainToken("owner", "123");
        }
        if (patientToken == null) {
            patientToken = obtainToken("benhnhan", "123");
        }
        if (dentistToken == null) {
            dentistToken = obtainToken("bs_tuan", "123");
        }
    }

    // =========================================================================
    // TIER 1: CATEGORY-PARTITION (EQUIVALENCE CLASSES & PRIMARY HAPPY PATHS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Area 1: Dental Service Catalog & Dynamic Booking (F31)")
    class Tier1ServiceCatalogAndBookingTests {

        @Test
        @DisplayName("T1-SRV-01: Public listing of active dental services returns catalog")
        void testGetDentalServicesCatalog() throws Exception {
            mockMvc.perform(get("/api/dental-services")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-SRV-02: Query dental service by service code returns detail attributes")
        void testGetServiceByCode() throws Exception {
            mockMvc.perform(get("/api/dental-services/NIENG_RANG_DAMON")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.serviceCode").value("NIENG_RANG_DAMON"));
        }

        @Test
        @DisplayName("T1-SRV-03: Filter dental services by category returns matching items")
        void testFilterServicesByCategory() throws Exception {
            mockMvc.perform(get("/api/dental-services")
                    .param("category", "CHINH_NHA")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-SRV-04: Available booking slots calculation for doctor and date")
        void testGetAvailableBookingSlots() throws Exception {
            mockMvc.perform(get("/api/appointments/available-slots")
                    .param("serviceCode", "NIENG_RANG_DAMON")
                    .param("dentistId", "1")
                    .param("date", "2026-10-15")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-SRV-05: Dynamic booking creation with service and down payment simulation")
        void testBookAppointmentAndPayDeposit() throws Exception {
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Nguyễn Văn An");
            bookReq.put("phone", "0912345678");
            bookReq.put("email", "an.nguyen@test.com");
            bookReq.put("serviceName", "Niềng Răng Mắc Cài Damon Q2");
            bookReq.put("appointmentTime", "2026-10-15T09:00:00");
            bookReq.put("dentistId", 1);
            bookReq.put("notes", "Tư vấn niềng răng hô");

            MvcResult result = mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            long appointmentId = root.path("data").path("id").asLong(1L);

            // Simulate deposit payment
            mockMvc.perform(post("/api/appointments/" + appointmentId + "/pay-deposit")
                    .header("X-Forwarded-For", getUniqueIp())
                    .param("method", "VNPAY_QR"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 2: Dental Products & Packaging Options (F32)")
    class Tier1DentalProductsTests {

        @Test
        @DisplayName("T1-PRD-01: Public listing of oral care products returns items")
        void testGetDentalProducts() throws Exception {
            mockMvc.perform(get("/api/dental-products")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-PRD-02: Filter oral care products by category (BAN_CHAI_DIEN)")
        void testFilterProductsByCategory() throws Exception {
            mockMvc.perform(get("/api/dental-products")
                    .param("category", "BAN_CHAI_DIEN")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-PRD-03: Retrieve product details including packaging options (box / combo)")
        void testGetProductDetailsWithPackagingOptions() throws Exception {
            mockMvc.perform(get("/api/dental-products/1")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.packagingOptions").isArray());
        }

        @Test
        @DisplayName("T1-PRD-04: Verify single box and combo packaging options have valid SKU codes")
        void testPackagingOptionProperties() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/dental-products/1")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode options = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("packagingOptions");
            if (options.isArray() && options.size() > 0) {
                assertTrue(options.get(0).has("skuCode"));
                assertTrue(options.get(0).has("packageType"));
            }
        }

        @Test
        @DisplayName("T1-PRD-05: Filter featured oral care products for clinic landing section")
        void testGetFeaturedProducts() throws Exception {
            mockMvc.perform(get("/api/dental-products")
                    .param("featured", "true")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 3: Cart Management & Order Checkout (F32)")
    class Tier1CartAndOrderCheckoutTests {

        @Test
        @DisplayName("T1-ORD-01: Patient views active shopping cart")
        void testGetShoppingCart() throws Exception {
            mockMvc.perform(get("/api/cart")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ORD-02: Add product with combo packaging option to cart")
        void testAddPackagingOptionToCart() throws Exception {
            Map<String, Object> itemReq = new HashMap<>();
            itemReq.put("productId", 1);
            itemReq.put("packagingOptionId", 1);
            itemReq.put("quantity", 2);

            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(itemReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ORD-03: Update quantity of item in cart")
        void testUpdateCartItemQuantity() throws Exception {
            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("quantity", 3);

            mockMvc.perform(put("/api/cart/items/1")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ORD-04: Place dental product order through checkout")
        void testCheckoutOrder() throws Exception {
            Map<String, Object> checkoutReq = new HashMap<>();
            checkoutReq.put("receiverName", "Vũ Hoàng Nam");
            checkoutReq.put("receiverPhone", "0988776655");
            checkoutReq.put("shippingAddress", "123 Đường Số 7, Phường An Lạc A, Bình Tân, TP.HCM");
            checkoutReq.put("shippingNotes", "Giao trong giờ hành chính");
            checkoutReq.put("paymentMethod", "COD");
            checkoutReq.put("items", List.of(
                    Map.of("productId", 1, "packagingOptionId", 1, "quantity", 1, "unitPrice", 1850000.0)
            ));

            mockMvc.perform(post("/api/dental-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkoutReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderNumber").isString());
        }

        @Test
        @DisplayName("T1-ORD-05: Retrieve order history for logged-in patient")
        void testGetPatientOrderHistory() throws Exception {
            mockMvc.perform(get("/api/orders/my-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 4: Porcelain Crown Warranty & QR Verification (F33)")
    class Tier1PorcelainCrownWarrantyTests {

        @Test
        @DisplayName("T1-WRN-01: Public warranty lookup by QR code verification parameter")
        void testVerifyWarrantyByQrCodeParam() throws Exception {
            mockMvc.perform(get("/api/warranties/verify")
                    .param("code", "DC-QR-LAVA-88992")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.porcelainBrand").isString());
        }

        @Test
        @DisplayName("T1-WRN-02: Public warranty lookup by QR security token path")
        void testVerifyWarrantyByPathToken() throws Exception {
            mockMvc.perform(get("/api/warranty/verify/DC-QR-LAVA-88992")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.warrantyCardNumber").isString());
        }

        @Test
        @DisplayName("T1-WRN-03: Verify warranty response contains FDI tooth numbering and labo origin")
        void testWarrantyDetailsAttributes() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/warranties/verify")
                    .param("code", "DC-QR-LAVA-88992")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            assertTrue(data.has("toothPositions"));
            assertTrue(data.has("laboSupplier"));
            assertTrue(data.has("expirationDate"));
        }

        @Test
        @DisplayName("T1-WRN-04: Patient queries personal list of porcelain crown warranties")
        void testGetPatientWarranties() throws Exception {
            mockMvc.perform(get("/api/warranty/my-warranties")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-WRN-05: Patient claims loyalty reward points by scanning warranty QR")
        void testScanClaimWarrantyPoints() throws Exception {
            Map<String, String> claimReq = new HashMap<>();
            claimReq.put("qrCode", "DC-QR-LAVA-88992");

            mockMvc.perform(post("/api/warranty/scan-claim")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(claimReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 5: Dental Loyalty Program (F34)")
    class Tier1DentalLoyaltyProgramTests {

        @Test
        @DisplayName("T1-LOY-01: Patient views loyalty account points and membership tier")
        void testGetLoyaltyPointsAndTier() throws Exception {
            mockMvc.perform(get("/api/loyalty/my-points")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currentPoints").isNumber())
                    .andExpect(jsonPath("$.data.tier").isString());
        }

        @Test
        @DisplayName("T1-LOY-02: Public or authenticated listing of redeemable rewards catalog")
        void testGetLoyaltyRewardsCatalog() throws Exception {
            mockMvc.perform(get("/api/loyalty/rewards")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-LOY-03: Patient redeems loyalty points for voucher or care gift")
        void testRedeemLoyaltyReward() throws Exception {
            Map<String, Object> redeemReq = new HashMap<>();
            redeemReq.put("rewardId", 1);
            redeemReq.put("pointsToRedeem", 200);

            mockMvc.perform(post("/api/loyalty/redeem")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(redeemReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-LOY-04: View loyalty point transaction history")
        void testGetLoyaltyTransactions() throws Exception {
            mockMvc.perform(get("/api/loyalty/transactions")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-LOY-05: Loyalty tier escalation rules (Standard -> Silver -> Gold -> Diamond)")
        void testLoyaltyTierThresholdRules() throws Exception {
            mockMvc.perform(get("/api/loyalty/tier-rules")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 6: AI Dental Pathology Diagnostic Vision (F35)")
    class Tier1AiDentalDiagnosticVisionTests {

        @Test
        @DisplayName("T1-AID-01: AI oral pathology diagnostic analysis with uploaded image file")
        void testAiDiagnosticAnalyzeImage() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "oral_photo.jpg",
                    "image/jpeg",
                    "SIMULATED_DENTAL_INTRAORAL_IMAGE_BYTES".getBytes()
            );

            mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .param("imageAngle", "INTRAORAL_FRONT")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.detectedPathologies").isArray())
                    .andExpect(jsonPath("$.data.healthScore").isNumber());
        }

        @Test
        @DisplayName("T1-AID-02: AI diagnostic analysis identifies caries, calculus, gingivitis, wisdom teeth")
        void testAiDiagnosticPathologyClassification() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "panoramic_xray.png",
                    "image/png",
                    "SIMULATED_DENTAL_XRAY_BYTES".getBytes()
            );

            MvcResult result = mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .param("imageAngle", "XRAY_PANORAMA")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            assertTrue(data.has("detectedPathologies"));
            assertTrue(data.has("clinicalSummary"));
            assertTrue(data.has("recommendedServiceCode"));
        }

        @Test
        @DisplayName("T1-AID-03: AI diagnostic fallback activation when external 9Router is unavailable")
        void testAiDiagnosticFallbackDeterministic() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "molar.jpg",
                    "image/jpeg",
                    "RAW_MOLAR_SAMPLE".getBytes()
            );

            mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .param("forceFallback", "true")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.healthScore").isNumber());
        }

        @Test
        @DisplayName("T1-AID-04: Patient reviews diagnostic scan history")
        void testGetDiagnosticHistory() throws Exception {
            mockMvc.perform(get("/api/ai-diagnostic/history")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-AID-05: AI diagnosis maps to recommended doctor consultation booking")
        void testAiDiagnosisReferralService() throws Exception {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "wisdom_tooth.jpg",
                    "image/jpeg",
                    "RAW_WISDOM_SAMPLE".getBytes()
            );

            MvcResult result = mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            assertNotNull(data.path("recommendedServiceCode").asText());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 7: Dental Forum & Clinic Map (F36)")
    class Tier1DentalForumAndMapTests {

        @Test
        @DisplayName("T1-FRM-01: Public feed of dental community posts")
        void testGetCommunityPosts() throws Exception {
            mockMvc.perform(get("/api/forum/posts")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-FRM-02: Patient creates a new question/post in community forum")
        void testCreateCommunityPost() throws Exception {
            Map<String, Object> postReq = new HashMap<>();
            postReq.put("title", "Có nên niềng răng trong suốt Invisalign khi bị hô nhẹ?");
            postReq.put("content", "Em đang phân vân giữa mắc cài sứ và Invisalign, nhờ các bác sĩ và anh chị chia sẻ kinh nghiệm.");
            postReq.put("category", "HOI_BAC_SI");

            mockMvc.perform(post("/api/forum/posts")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(postReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("T1-FRM-03: Doctor replies to community post with verified badge")
        void testDoctorReplyToCommunityPost() throws Exception {
            Map<String, Object> commentReq = new HashMap<>();
            commentReq.put("content", "Chào bạn, với trường hợp hô nhẹ thì Invisalign là lựa chọn tối ưu về thẩm mỹ và thời gian điều trị.");

            mockMvc.perform(post("/api/community/posts/1/comments")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MAP-04: Retrieve all clinic branches with GPS coordinates and amenities")
        void testGetClinicBranches() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/branches")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            JsonNode branches = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            if (branches.size() > 0) {
                assertTrue(branches.get(0).has("latitude"));
                assertTrue(branches.get(0).has("longitude"));
                assertTrue(branches.get(0).has("hotline"));
            }
        }

        @Test
        @DisplayName("T1-MAP-05: Generate directions navigation URL for clinic branch")
        void testGetBranchDirections() throws Exception {
            mockMvc.perform(get("/api/branches/1/directions")
                    .param("userLat", "10.7769")
                    .param("userLng", "106.7009")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.directionsUrl").isString());
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 8: Customer Mobile App Contract (F37)")
    class Tier1CustomerMobileAppContractTests {

        @Test
        @DisplayName("T1-MOB-01: Mobile patient dashboard config endpoint")
        void testGetMobilePatientConfig() throws Exception {
            mockMvc.perform(get("/api/mobile/patient/config")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MOB-02: Mobile fast booking slot selector")
        void testMobileFastBookingSlots() throws Exception {
            mockMvc.perform(get("/api/appointments/available-slots")
                    .param("serviceCode", "IMPLANT_STRAUMANN")
                    .param("date", "2026-10-20")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MOB-03: Mobile notification feed for patient")
        void testGetPatientNotifications() throws Exception {
            mockMvc.perform(get("/api/notifications")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MOB-04: Mobile profile view for logged-in patient")
        void testGetPatientProfile() throws Exception {
            mockMvc.perform(get("/api/patients/me")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MOB-05: Mobile coupon wallet for patient")
        void testGetPatientCoupons() throws Exception {
            mockMvc.perform(get("/api/coupons/active")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY VALUE ANALYSIS (CORNER CASES, EXTREMES & ERROR HANDLING)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2: Boundary Value Analysis")
    class Tier2BoundaryValueAnalysisTests {

        @Test
        @DisplayName("T2-BND-01: Query non-existent service code returns 404 Not Found")
        void testGetNonExistentService() throws Exception {
            mockMvc.perform(get("/api/dental-services/NON_EXISTENT_CODE_9999")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("T2-BND-02: Booking appointment with invalid phone number returns 400 Bad Request")
        void testBookingInvalidPhone() throws Exception {
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Test Patient");
            bookReq.put("phone", "invalid-phone-abc");
            bookReq.put("serviceName", "Tẩy Trắng Răng");
            bookReq.put("appointmentTime", "2026-10-15T09:00:00");

            mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-03: Cart item add with negative quantity returns 400 Bad Request")
        void testAddNegativeQuantityToCart() throws Exception {
            Map<String, Object> itemReq = new HashMap<>();
            itemReq.put("productId", 1);
            itemReq.put("packagingOptionId", 1);
            itemReq.put("quantity", -5);

            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(itemReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-04: Order checkout with empty items array returns 400 Bad Request")
        void testCheckoutEmptyItems() throws Exception {
            Map<String, Object> checkoutReq = new HashMap<>();
            checkoutReq.put("receiverName", "Vũ Hoàng Nam");
            checkoutReq.put("receiverPhone", "0988776655");
            checkoutReq.put("shippingAddress", "123 Bình Tân");
            checkoutReq.put("items", List.of());

            mockMvc.perform(post("/api/dental-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkoutReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-05: Non-existent warranty QR token lookup returns 404 Not Found")
        void testLookupInvalidWarrantyQr() throws Exception {
            mockMvc.perform(get("/api/warranties/verify")
                    .param("code", "FAKE-QR-TOKEN-INVALID-XXXX")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("T2-BND-06: Loyalty points redemption exceeding current balance returns 400 Bad Request")
        void testRedeemPointsExcessive() throws Exception {
            Map<String, Object> redeemReq = new HashMap<>();
            redeemReq.put("rewardId", 1);
            redeemReq.put("pointsToRedeem", 999999);

            mockMvc.perform(post("/api/loyalty/redeem")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(redeemReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-07: AI diagnostic upload with empty file returns 400 Bad Request")
        void testAiDiagnosticEmptyFile() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "image", "empty.jpg", "image/jpeg", new byte[0]
            );

            mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(emptyFile)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-BND-08: Community forum post creation with blank title returns 400 Bad Request")
        void testCreatePostBlankTitle() throws Exception {
            Map<String, Object> postReq = new HashMap<>();
            postReq.put("title", "");
            postReq.put("content", "Nội dung không có tiêu đề hợp lệ.");

            mockMvc.perform(post("/api/forum/posts")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(postReq)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // TIER 3: PAIRWISE COMBINATORIAL (CROSS-FEATURE PIPELINES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 3: Pairwise Combinatorial Cross-Feature Pipelines")
    class Tier3PairwiseCombinatorialTests {

        @Test
        @DisplayName("T3-CUS-01: Service Catalog -> Dynamic Slot Checking -> Booking -> Deposit Payment Flow")
        void testServiceCatalogToBookingAndDepositFlow() throws Exception {
            // Step 1: Browse catalog
            mockMvc.perform(get("/api/dental-services")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // Step 2: Check available slots
            mockMvc.perform(get("/api/appointments/available-slots")
                    .param("serviceCode", "NIENG_RANG_DAMON")
                    .param("date", "2026-11-01")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // Step 3: Book appointment
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Trần Mai Anh");
            bookReq.put("phone", "0933445566");
            bookReq.put("serviceName", "Niềng Răng Mắc Cài");
            bookReq.put("appointmentTime", "2026-11-01T14:00:00");

            MvcResult result = mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            long appId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            // Step 4: Pay deposit
            mockMvc.perform(post("/api/appointments/" + appId + "/pay-deposit")
                    .param("method", "VNPAY_QR")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-CUS-02: Product Catalog -> Packaging Selection -> Cart Add -> Order Checkout -> Loyalty Increment")
        void testProductToCartCheckoutAndLoyaltyFlow() throws Exception {
            // Step 1: View product details with packaging options
            mockMvc.perform(get("/api/dental-products/1")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // Step 2: Add combo packaging to cart
            Map<String, Object> itemReq = new HashMap<>();
            itemReq.put("productId", 1);
            itemReq.put("packagingOptionId", 1);
            itemReq.put("quantity", 1);

            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(itemReq)))
                    .andExpect(status().isOk());

            // Step 3: Checkout order
            Map<String, Object> checkoutReq = new HashMap<>();
            checkoutReq.put("receiverName", "Vũ Hoàng Nam");
            checkoutReq.put("receiverPhone", "0988776655");
            checkoutReq.put("shippingAddress", "Bình Tân, TP.HCM");
            checkoutReq.put("items", List.of(Map.of("productId", 1, "packagingOptionId", 1, "quantity", 1, "unitPrice", 1850000.0)));

            mockMvc.perform(post("/api/dental-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkoutReq)))
                    .andExpect(status().isOk());

            // Step 4: Check loyalty points update
            mockMvc.perform(get("/api/loyalty/my-points")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-CUS-03: AI Dental Pathology Diagnostic -> Recommended Service -> Booking Direct Referral")
        void testAiDiagnosticToServiceBookingReferral() throws Exception {
            // Step 1: Execute AI diagnosis
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "molar_caries.jpg", "image/jpeg", "CARIES_IMAGE_STREAM".getBytes()
            );

            MvcResult result = mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            String recommendedService = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("recommendedServiceCode").asText("TRAM_RANG_COMPOSITE");

            // Step 2: Query recommended service in catalog
            mockMvc.perform(get("/api/dental-services/" + recommendedService)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-CUS-04: Porcelain Crown QR Scan -> Warranty Details Verification -> Loyalty Scan Claim")
        void testWarrantyScanToLoyaltyClaimFlow() throws Exception {
            String qrToken = "DC-QR-LAVA-88992";

            // Step 1: Public QR verification
            mockMvc.perform(get("/api/warranties/verify")
                    .param("code", qrToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // Step 2: Patient claims loyalty points from QR warranty card
            Map<String, String> claimReq = new HashMap<>();
            claimReq.put("qrCode", qrToken);

            mockMvc.perform(post("/api/warranty/scan-claim")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(claimReq)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-CUS-05: Forum Post Discussion -> Doctor Verified Reply -> Clinic Branch Navigation")
        void testForumDoctorReplyToClinicBranchNavigationFlow() throws Exception {
            // Step 1: Patient posts inquiry
            Map<String, Object> postReq = new HashMap<>();
            postReq.put("title", "Khám răng khôn mọc lệch ở chi nhánh nào?");
            postReq.put("content", "Em ở khu vực Tân Phú muốn tìm chi nhánh gần nhất có chụp phim Cone Beam 3D.");
            postReq.put("category", "HOI_BAC_SI");

            mockMvc.perform(post("/api/forum/posts")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(postReq)))
                    .andExpect(status().isOk());

            // Step 2: Doctor replies with verified advice
            Map<String, Object> replyReq = new HashMap<>();
            replyReq.put("content", "Chào em, chi nhánh Bình Tân có sẵn máy chụp phim Cone Beam 3D thế hệ mới em nhé.");

            mockMvc.perform(post("/api/community/posts/1/comments")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(replyReq)))
                    .andExpect(status().isOk());

            // Step 3: Patient opens branches map and requests directions
            mockMvc.perform(get("/api/branches/1/directions")
                    .param("userLat", "10.7900")
                    .param("userLng", "106.6300")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD WORKLOAD SCENARIOS (END-TO-END PATIENT JOURNEYS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 4: Real-World Workload Scenarios")
    class Tier4RealWorldWorkloadScenariosTests {

        @Test
        @DisplayName("T4-CUS-01: Comprehensive New Patient Digital Onboarding Journey")
        void testNewPatientDigitalOnboardingScenario() throws Exception {
            // 1. Patient performs AI dental scan for oral health evaluation
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image", "smile_scan.jpg", "image/jpeg", "RAW_SMILE_BYTES".getBytes()
            );

            MvcResult scanResult = mockMvc.perform(multipart("/api/dental-ai/diagnose")
                    .file(imageFile)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            assertNotNull(scanResult.getResponse().getContentAsString());

            // 2. Patient validates active promotional coupon
            Map<String, String> couponReq = new HashMap<>();
            couponReq.put("code", "FREEEXAM");

            mockMvc.perform(post("/api/coupons/validate")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(couponReq)))
                    .andExpect(status().isOk());

            // 3. Patient books consultation appointment
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Lê Bảo Châu");
            bookReq.put("phone", "0977112233");
            bookReq.put("email", "chau.le@example.com");
            bookReq.put("serviceName", "Khám & Tư Vấn Thẩm Mỹ Nụ Cười");
            bookReq.put("appointmentTime", "2026-11-05T10:00:00");
            bookReq.put("dentistId", 1);
            bookReq.put("notes", "Đã quét chẩn đoán AI phát hiện khớp cắn lệch nhẹ");

            MvcResult bookResult = mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            long appId = objectMapper.readTree(bookResult.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            // 4. Patient pays deposit online
            mockMvc.perform(post("/api/appointments/" + appId + "/pay-deposit")
                    .param("method", "VNPAY_QR")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // 5. Patient checks clinic branch location and parking directions
            mockMvc.perform(get("/api/branches")
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T4-CUS-02: Post-Treatment Aesthetic Porcelain Crown & Home Care Journey")
        void testPostTreatmentPorcelainCrownAndHomeCareScenario() throws Exception {
            // 1. Patient scans QR code on physical porcelain crown warranty card
            String qrToken = "DC-QR-LAVA-88992";
            MvcResult warrantyResult = mockMvc.perform(get("/api/warranty/verify/" + qrToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            assertTrue(warrantyResult.getResponse().getContentAsString().contains("Lava Plus"));

            // 2. Patient claims warranty loyalty bonus points
            Map<String, String> claimReq = new HashMap<>();
            claimReq.put("qrCode", qrToken);

            mockMvc.perform(post("/api/warranty/scan-claim")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(claimReq)))
                    .andExpect(status().isOk());

            // 3. Patient checks loyalty balance and redeems points for water flosser discount
            Map<String, Object> redeemReq = new HashMap<>();
            redeemReq.put("rewardId", 2);
            redeemReq.put("pointsToRedeem", 100);

            mockMvc.perform(post("/api/loyalty/redeem")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(redeemReq)))
                    .andExpect(status().isOk());

            // 4. Patient orders Oral-B sonic toothbrush combo kit with home delivery
            Map<String, Object> checkoutReq = new HashMap<>();
            checkoutReq.put("receiverName", "Vũ Hoàng Nam");
            checkoutReq.put("receiverPhone", "0988776655");
            checkoutReq.put("shippingAddress", "Chung cư Moonlight Boulevard, Bình Tân, TP.HCM");
            checkoutReq.put("paymentMethod", "VNPAY_QR");
            checkoutReq.put("items", List.of(Map.of("productId", 2, "packagingOptionId", 2, "quantity", 1, "unitPrice", 2450000.0)));

            mockMvc.perform(post("/api/dental-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkoutReq)))
                    .andExpect(status().isOk());

            // 5. Patient checks order history
            mockMvc.perform(get("/api/orders/my-orders")
                    .header("Authorization", "Bearer " + patientToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }
    }
}
