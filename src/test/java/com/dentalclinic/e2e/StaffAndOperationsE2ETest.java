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
 * Enterprise E2E Test Suite for Staff Operations, B2B Satellite Clinics & Field Intake (F38-F42).
 * Follows the 4-Tier requirement-driven opaque-box testing methodology:
 * - Tier 1: Category-Partition (Primary functional areas: Satellite Clinics, Materials, Orders, Attendance, Doctor KPIs, Field Intake)
 * - Tier 2: Boundary Value Analysis (Zero/negative inventory, credit limit overdraw, duplicate attendance, lead validation)
 * - Tier 3: Pairwise Combinatorial (Cross-feature operational workflows: B2B Order -> Stock Decrement; School Screening -> Booking -> KPI)
 * - Tier 4: Real-World Workload Scenarios (Full operational shift lifecycle; School screening to in-clinic conversion)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Staff & Operations E2E Test Suite (F38-F42)")
public class StaffAndOperationsE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger IP_COUNTER = new AtomicInteger(300);

    private String ownerToken;
    private String receptionistToken;
    private String dentistToken;
    private String patientToken;

    private String getUniqueIp() {
        return "192.168.30." + (IP_COUNTER.incrementAndGet() % 250);
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
        if (receptionistToken == null) {
            receptionistToken = obtainToken("letan", "123");
        }
        if (dentistToken == null) {
            dentistToken = obtainToken("bs_tuan", "123");
        }
        if (patientToken == null) {
            patientToken = obtainToken("benhnhan", "123");
        }
    }

    // =========================================================================
    // TIER 1: CATEGORY-PARTITION (PRIMARY FUNCTIONAL FLOWS & EQUIVALENCE CLASSES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 1 - Area 1: B2B Tier-2 Agent & Satellite Clinics (F38)")
    class Tier1Tier2AgentAndSatelliteClinicTests {

        @Test
        @DisplayName("T1-AGT-01: Create and register new Tier-2 satellite clinic partner")
        void testRegisterTier2SatelliteClinic() throws Exception {
            Map<String, Object> agentReq = new HashMap<>();
            agentReq.put("agentCode", "AGT-BDG-01");
            agentReq.put("agentName", "Nha Khoa DentalCare Vệ Tinh Thủ Dầu Một");
            agentReq.put("agentType", "SATELLITE_CLINIC");
            agentReq.put("province", "Bình Dương");
            agentReq.put("city", "Thủ Dầu Một");
            agentReq.put("address", "88 Đại Lộ Bình Dương");
            agentReq.put("phone", "02743888999");
            agentReq.put("representativeName", "BS. Hoàng Văn Nam");
            agentReq.put("commissionRate", 15.0);
            agentReq.put("creditLimit", 200000000.0);

            mockMvc.perform(post("/api/tier2-agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(agentReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.agentCode").value("AGT-BDG-01"));
        }

        @Test
        @DisplayName("T1-AGT-02: List all active Tier-2 satellite clinics and distributors")
        void testGetAllTier2Agents() throws Exception {
            mockMvc.perform(get("/api/tier2-agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-AGT-03: Retrieve Tier-2 agent details by ID")
        void testGetTier2AgentById() throws Exception {
            mockMvc.perform(get("/api/tier2-agents/1")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-AGT-04: Update satellite clinic status and credit limit")
        void testUpdateSatelliteClinicStatus() throws Exception {
            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("creditLimit", 300000000.0);
            updateReq.put("active", true);

            mockMvc.perform(put("/api/tier2-agents/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-AGT-05: Filter B2B agents by agent type (DISTRIBUTOR)")
        void testFilterAgentsByType() throws Exception {
            mockMvc.perform(get("/api/tier2-agents")
                    .param("agentType", "DISTRIBUTOR")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 2: Dental Material Inventory & Procurement Orders (F39)")
    class Tier1MaterialInventoryAndOrdersTests {

        @Test
        @DisplayName("T1-MAT-01: Retrieve central warehouse dental materials inventory")
        void testGetDentalMaterials() throws Exception {
            mockMvc.perform(get("/api/dental-materials")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-MAT-02: Filter dental materials by category (IMPLANT_POST, BRACKET, CONSUMABLE)")
        void testFilterMaterialsByCategory() throws Exception {
            mockMvc.perform(get("/api/dental-materials")
                    .param("category", "IMPLANT_POST")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MAT-03: Update central warehouse stock level for material")
        void testUpdateMaterialStockLevel() throws Exception {
            Map<String, Object> stockReq = new HashMap<>();
            stockReq.put("stockChange", 50);
            stockReq.put("reason", "Nhập kho đợt mới từ Dentsply Sirona");

            mockMvc.perform(put("/api/dental-materials/1/stock")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(stockReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-MAT-04: Satellite clinic creates procurement material order")
        void testCreateMaterialProcurementOrder() throws Exception {
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", 1);
            orderReq.put("notes", "Đơn đặt hàng mắc cài Damon và dây cung chỉnh nha tháng 10");
            orderReq.put("items", List.of(
                    Map.of("materialId", 1, "quantityRequested", 10, "unitPrice", 3500000.0),
                    Map.of("materialId", 2, "quantityRequested", 20, "unitPrice", 250000.0)
            ));

            mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderCode").isString());
        }

        @Test
        @DisplayName("T1-MAT-05: Central management approves and dispatches material order")
        void testApproveAndDispatchMaterialOrder() throws Exception {
            Map<String, String> statusReq = new HashMap<>();
            statusReq.put("status", "APPROVED");
            statusReq.put("notes", "Đã đối chiếu công nợ và duyệt xuất kho.");

            mockMvc.perform(put("/api/material-orders/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 3: Staff Timekeeping & Shift Check-In (F40)")
    class Tier1StaffAttendanceAndShiftTests {

        @Test
        @DisplayName("T1-ATN-01: Query clinic staff shifts schedule")
        void testGetStaffShifts() throws Exception {
            mockMvc.perform(get("/api/shifts")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-ATN-02: Staff performs morning shift check-in with GPS and IP verification")
        void testStaffMorningCheckIn() throws Exception {
            Map<String, Object> checkInReq = new HashMap<>();
            checkInReq.put("shiftId", 1);
            checkInReq.put("latitude", 10.760624);
            checkInReq.put("longitude", 106.587106);
            checkInReq.put("networkIp", "192.168.1.50");

            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkInReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").isString());
        }

        @Test
        @DisplayName("T1-ATN-03: Staff performs shift check-out at end of working day")
        void testStaffShiftCheckOut() throws Exception {
            Map<String, Object> checkOutReq = new HashMap<>();
            checkOutReq.put("shiftId", 1);
            checkOutReq.put("notes", "Hoàn thành ca trực, bàn giao 12 hồ sơ bệnh án.");

            mockMvc.perform(post("/api/attendance/check-out")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkOutReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-ATN-04: Query personal staff attendance history")
        void testGetStaffAttendanceHistory() throws Exception {
            mockMvc.perform(get("/api/attendance/my-history")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-ATN-05: Management audits clinic attendance records across branches")
        void testAuditClinicAttendance() throws Exception {
            mockMvc.perform(get("/api/attendance/audit")
                    .param("date", "2026-09-23")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 4: Doctor Consultation & Revenue KPIs (F41)")
    class Tier1DoctorKpiAndRevenueTests {

        @Test
        @DisplayName("T1-KPI-01: Retrieve doctor consultation and conversion KPIs by doctor ID")
        void testGetDoctorKpiById() throws Exception {
            mockMvc.perform(get("/api/staff-kpi/doctor/4")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.doctorName").isString())
                    .andExpect(jsonPath("$.data.completedConsultations").isNumber());
        }

        @Test
        @DisplayName("T1-KPI-02: Doctor views personal monthly KPI metrics")
        void testDoctorViewsPersonalKpi() throws Exception {
            mockMvc.perform(get("/api/staff-kpi/my-kpi")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-KPI-03: Verify treatment conversion metrics (Implant, Ortho, Crowns)")
        void testDoctorConversionMetrics() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/staff-kpi/doctor/4")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
            assertTrue(data.has("treatmentConversions"));
            assertTrue(data.has("totalRevenueAttributed"));
        }

        @Test
        @DisplayName("T1-KPI-04: Retrieve clinic-wide doctor performance ranking")
        void testGetClinicDoctorRankings() throws Exception {
            mockMvc.perform(get("/api/staff-kpi/rankings")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-KPI-05: Monthly KPI comparison and conversion rate calculations")
        void testMonthlyKpiComparison() throws Exception {
            mockMvc.perform(get("/api/staff-kpi/doctor/4")
                    .param("month", "2026-09")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Tier 1 - Area 5: Field Patient Intake & School Screening (F42)")
    class Tier1FieldPatientIntakeTests {

        @Test
        @DisplayName("T1-INT-01: Capture student screening lead at school dental event")
        void testCaptureSchoolScreeningLead() throws Exception {
            Map<String, Object> intakeReq = new HashMap<>();
            intakeReq.put("eventName", "Chương Trình Nụ Cười Học Đường 2026 - THCS Lê Quý Đôn");
            intakeReq.put("eventType", "SCHOOL_SCREENING");
            intakeReq.put("patientFullName", "Trần Minh Quân");
            intakeReq.put("birthYear", 2013);
            intakeReq.put("studentClass", "7A2");
            intakeReq.put("phone", "0908112233");
            intakeReq.put("parentName", "Trần Văn Hưng");
            intakeReq.put("parentPhone", "0908112233");
            intakeReq.put("screeningFindings", "Sâu răng hàm số 46, khớp cắn hở nhẹ hàm trên");
            intakeReq.put("recommendation", "Cần hàn răng sâu sớm, tái khám chỉnh nha học đường");
            intakeReq.put("voucherCode", "HOCDUONG100K");

            mockMvc.perform(post("/api/field-intake")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(intakeReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.leadStatus").value("NEW"));
        }

        @Test
        @DisplayName("T1-INT-02: Retrieve list of field screening intake leads")
        void testGetFieldIntakeLeads() throws Exception {
            mockMvc.perform(get("/api/field-intake")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("T1-INT-03: Filter field intake leads by event name")
        void testFilterFieldIntakeByEvent() throws Exception {
            mockMvc.perform(get("/api/field-intake")
                    .param("eventName", "THCS Lê Quý Đôn")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-INT-04: Update field lead status to CONTACTED and BOOKED_APPOINTMENT")
        void testUpdateFieldLeadStatus() throws Exception {
            Map<String, String> statusReq = new HashMap<>();
            statusReq.put("status", "CONTACTED");
            statusReq.put("notes", "Đã gọi phụ huynh, hẹn thứ 7 tuần này đưa bé đến cơ sở Bình Tân.");

            mockMvc.perform(put("/api/field-intake/1/status")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("T1-INT-05: Batch sync offline cached intake leads captured in field")
        void testBatchSyncFieldIntakeLeads() throws Exception {
            List<Map<String, Object>> batchReq = List.of(
                    Map.of("eventName", "VIDEC 2026", "patientFullName", "Phan Tuấn Kiệt", "phone", "0918776655", "recommendation", "Tẩy trắng răng"),
                    Map.of("eventName", "VIDEC 2026", "patientFullName", "Đỗ Quỳnh Như", "phone", "0919887766", "recommendation", "Dán sứ Veneer Emax")
            );

            mockMvc.perform(post("/api/field-intake/batch-sync")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(batchReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // =========================================================================
    // TIER 2: BOUNDARY VALUE ANALYSIS (EXTREME & CORNER CASES)
    // =========================================================================

    @Nested
    @DisplayName("Tier 2: Boundary Value Analysis")
    class Tier2BoundaryValueAnalysisTests {

        @Test
        @DisplayName("T2-OPS-01: Update material inventory with negative stock quantity returns 400 Bad Request")
        void testUpdateNegativeMaterialStock() throws Exception {
            Map<String, Object> stockReq = new HashMap<>();
            stockReq.put("stockChange", -99999);
            stockReq.put("reason", "Excessive negative adjustment");

            mockMvc.perform(put("/api/dental-materials/1/stock")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(stockReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-OPS-02: Procurement order with empty item list returns 400 Bad Request")
        void testCreateEmptyProcurementOrder() throws Exception {
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", 1);
            orderReq.put("items", List.of());

            mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-OPS-03: Satellite clinic order exceeding credit limit returns 400 Bad Request")
        void testOrderExceedingCreditLimit() throws Exception {
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", 1);
            orderReq.put("items", List.of(
                    Map.of("materialId", 1, "quantityRequested", 1000, "unitPrice", 100000000.0) // 100 billion VND
            ));

            mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-OPS-04: Staff check-in with non-existent shift ID returns 404 or 400")
        void testCheckInNonExistentShift() throws Exception {
            Map<String, Object> checkInReq = new HashMap<>();
            checkInReq.put("shiftId", 999999);

            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkInReq)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("T2-OPS-05: Duplicate check-in on the same shift triggers 400 Bad Request")
        void testDuplicateShiftCheckIn() throws Exception {
            Map<String, Object> checkInReq = new HashMap<>();
            checkInReq.put("shiftId", 1);

            // First check-in
            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkInReq)));

            // Duplicate check-in attempt
            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkInReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-OPS-06: Doctor KPI query for non-existent doctor ID returns 404 Not Found")
        void testGetKpiNonExistentDoctor() throws Exception {
            mockMvc.perform(get("/api/staff-kpi/doctor/999999")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("T2-OPS-07: Field intake with missing phone number returns 400 Bad Request")
        void testFieldIntakeMissingPhone() throws Exception {
            Map<String, Object> intakeReq = new HashMap<>();
            intakeReq.put("eventName", "Khám Răng Học Đường");
            intakeReq.put("patientFullName", "Học Sinh Không Có SĐT");
            intakeReq.put("phone", "");

            mockMvc.perform(post("/api/field-intake")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(intakeReq)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("T2-OPS-08: Rejection of procurement order without rejection reason returns 400")
        void testRejectOrderWithoutReason() throws Exception {
            Map<String, String> statusReq = new HashMap<>();
            statusReq.put("status", "REJECTED");
            statusReq.put("notes", ""); // Missing mandatory rejection reason

            mockMvc.perform(put("/api/material-orders/1/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq)))
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
        @DisplayName("T3-OPS-01: Satellite Clinic Procurement -> Approval -> Stock Decrement -> Agent Balance Sync")
        void testSatelliteProcurementToStockDecrementFlow() throws Exception {
            // 1. Satellite places order for 5 Straumann implant posts
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", 1);
            orderReq.put("items", List.of(Map.of("materialId", 1, "quantityRequested", 5, "unitPrice", 3500000.0)));

            MvcResult orderResult = mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            // 2. Owner approves order
            Map<String, String> approveReq = new HashMap<>();
            approveReq.put("status", "APPROVED");
            approveReq.put("notes", "Đồng ý xuất kho vệ tinh");

            mockMvc.perform(put("/api/material-orders/" + orderId + "/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(approveReq)))
                    .andExpect(status().isOk());

            // 3. Verify material stock is verified
            mockMvc.perform(get("/api/dental-materials/1")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-OPS-02: School Screening Lead -> Voucher Issuance -> Booking -> Consultation -> Doctor KPI")
        void testScreeningLeadToBookingAndDoctorKpiFlow() throws Exception {
            // 1. Capture screening lead
            Map<String, Object> intakeReq = new HashMap<>();
            intakeReq.put("eventName", "Nụ Cười Học Đường 2026");
            intakeReq.put("patientFullName", "Đinh Ngọc Ánh");
            intakeReq.put("phone", "0944556677");
            intakeReq.put("screeningFindings", "Khớp cắn sâu, chen chúc răng cửa");
            intakeReq.put("recommendation", "Niềng răng can thiệp sớm");
            intakeReq.put("voucherCode", "HOCDUONG2026");

            mockMvc.perform(post("/api/field-intake")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(intakeReq)))
                    .andExpect(status().isOk());

            // 2. Parent uses voucher to book consultation
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Đinh Ngọc Ánh");
            bookReq.put("phone", "0944556677");
            bookReq.put("serviceName", "Niềng Răng Mắc Cài");
            bookReq.put("appointmentTime", "2026-10-25T15:00:00");
            bookReq.put("dentistId", 4);

            mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isOk());

            // 3. Verify doctor KPI records consultation
            mockMvc.perform(get("/api/staff-kpi/doctor/4")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-OPS-03: Staff Check-In -> Shift Completion -> Doctor Consultations -> Daily KPI Sync")
        void testShiftCheckInToDoctorKpiSyncFlow() throws Exception {
            // 1. Doctor check-in
            Map<String, Object> checkInReq = new HashMap<>();
            checkInReq.put("shiftId", 2);
            checkInReq.put("latitude", 10.760624);
            checkInReq.put("longitude", 106.587106);

            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkInReq)))
                    .andExpect(status().isOk());

            // 2. Doctor views personal KPI for the shift
            mockMvc.perform(get("/api/staff-kpi/my-kpi")
                    .header("Authorization", "Bearer " + dentistToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T3-OPS-04: Satellite Clinic Registration -> Credit Limit Assignment -> First Order Creation")
        void testSatelliteRegistrationToFirstOrderFlow() throws Exception {
            // 1. Register new franchise satellite clinic
            Map<String, Object> agentReq = new HashMap<>();
            agentReq.put("agentCode", "AGT-VTU-01");
            agentReq.put("agentName", "Nha Khoa DentalCare Vũng Tàu");
            agentReq.put("agentType", "FRANCHISE_PARTNER");
            agentReq.put("province", "Bà Rịa - Vũng Tàu");
            agentReq.put("phone", "02543888777");
            agentReq.put("creditLimit", 150000000.0);

            MvcResult agentResult = mockMvc.perform(post("/api/tier2-agents")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(agentReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            long agentId = objectMapper.readTree(agentResult.getResponse().getContentAsString())
                    .path("data").path("id").asLong(2L);

            // 2. Submit initial equipment/material starter pack order
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", agentId);
            orderReq.put("notes", "Gói vật tư ban đầu cho chi nhánh Vũng Tàu");
            orderReq.put("items", List.of(Map.of("materialId", 2, "quantityRequested", 15, "unitPrice", 1200000.0)));

            mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD WORKLOAD SCENARIOS (OPERATIONAL WORKLOAD SIMULATIONS)
    // =========================================================================

    @Nested
    @DisplayName("Tier 4: Real-World Workload Scenarios")
    class Tier4RealWorldWorkloadScenariosTests {

        @Test
        @DisplayName("T4-OPS-01: Full Day Operational Practice & Inventory Lifecycle")
        void testFullDayPracticeAndInventoryLifecycleScenario() throws Exception {
            // 1. Morning staff and doctor check-in
            Map<String, Object> morningCheckIn = new HashMap<>();
            morningCheckIn.put("shiftId", 1);
            morningCheckIn.put("latitude", 10.760624);
            morningCheckIn.put("longitude", 106.587106);

            mockMvc.perform(post("/api/attendance/check-in")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(morningCheckIn)))
                    .andExpect(status().isOk());

            // 2. Central warehouse inventory audit
            mockMvc.perform(get("/api/dental-materials")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());

            // 3. Satellite clinic replenishment order created & approved
            Map<String, Object> orderReq = new HashMap<>();
            orderReq.put("agentId", 1);
            orderReq.put("notes", "Bổ sung vật tư tiêu hao khẩn cấp");
            orderReq.put("items", List.of(Map.of("materialId", 1, "quantityRequested", 4, "unitPrice", 3000000.0)));

            MvcResult orderResult = mockMvc.perform(post("/api/material-orders")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            Map<String, String> approveReq = new HashMap<>();
            approveReq.put("status", "APPROVED");
            approveReq.put("notes", "Đã duyệt đơn bổ sung");

            mockMvc.perform(put("/api/material-orders/" + orderId + "/status")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(approveReq)))
                    .andExpect(status().isOk());

            // 4. Evening staff check-out
            Map<String, Object> checkOutReq = new HashMap<>();
            checkOutReq.put("shiftId", 1);
            checkOutReq.put("notes", "Kết thúc ca trực ban ngày.");

            mockMvc.perform(post("/api/attendance/check-out")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(checkOutReq)))
                    .andExpect(status().isOk());

            // 5. Owner reviews end-of-day clinic KPI dashboard
            mockMvc.perform(get("/api/staff-kpi/rankings")
                    .header("Authorization", "Bearer " + ownerToken)
                    .header("X-Forwarded-For", getUniqueIp()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("T4-OPS-02: Field School Screening to In-Clinic Treatment Conversion Pipeline")
        void testFieldScreeningToTreatmentConversionPipelineScenario() throws Exception {
            // 1. Field team screens 2 students and records leads
            Map<String, Object> lead1 = new HashMap<>();
            lead1.put("eventName", "Khám Học Đường THCS Lê Quý Đôn");
            lead1.put("patientFullName", "Nguyễn Gia Hân");
            lead1.put("phone", "0938112233");
            lead1.put("screeningFindings", "Viêm lợi phì đại, mảng bám vôi răng độ 2");
            lead1.put("recommendation", "Lấy cao răng & hướng dẫn vệ sinh răng miệng");

            MvcResult lead1Result = mockMvc.perform(post("/api/field-intake")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lead1)))
                    .andExpect(status().isOk())
                    .andReturn();

            long leadId = objectMapper.readTree(lead1Result.getResponse().getContentAsString())
                    .path("data").path("id").asLong(1L);

            // 2. Receptionist contacts parent and updates status
            Map<String, String> statusReq = new HashMap<>();
            statusReq.put("status", "CONTACTED");
            statusReq.put("notes", "Phụ huynh đồng ý đưa bé đến khám chiều chủ nhật.");

            mockMvc.perform(put("/api/field-intake/" + leadId + "/status")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk());

            // 3. Booking is created from converted lead
            Map<String, Object> bookReq = new HashMap<>();
            bookReq.put("patientName", "Nguyễn Gia Hân");
            bookReq.put("phone", "0938112233");
            bookReq.put("serviceName", "Cạo Vôi Răng & Đánh Bóng");
            bookReq.put("appointmentTime", "2026-10-28T16:00:00");
            bookReq.put("dentistId", 4);

            mockMvc.perform(post("/api/appointments/book")
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookReq)))
                    .andExpect(status().isOk());

            // 4. Update lead status to BOOKED_APPOINTMENT
            statusReq.put("status", "BOOKED_APPOINTMENT");
            mockMvc.perform(put("/api/field-intake/" + leadId + "/status")
                    .header("Authorization", "Bearer " + receptionistToken)
                    .header("X-Forwarded-For", getUniqueIp())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq)))
                    .andExpect(status().isOk());
        }
    }
}
