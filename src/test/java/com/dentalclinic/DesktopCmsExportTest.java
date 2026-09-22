package com.dentalclinic;

import com.dentalclinic.dto.CmsConfigDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DesktopCmsExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. CMS Config: Tra cứu cấu hình động Menu & Footer của phòng khám")
    void testGetCmsConfig() throws Exception {
        mockMvc.perform(get("/api/cms/config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clinicName").value("DentalCare Luxury Dental Clinic"))
                .andExpect(jsonPath("$.data.hotline").value("1900 6868"))
                .andExpect(jsonPath("$.data.menuItems", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data.footerConfig.licenseNumber").exists());
    }

    @Test
    @DisplayName("2. CMS Config: Cập nhật thông tin hotline và thương hiệu phòng khám")
    void testUpdateCmsConfig() throws Exception {
        CmsConfigDto config = CmsConfigDto.createDefault();
        config.setClinicName("DentalCare Luxury Command Center Premium");
        config.setHotline("1800 9999");
        config.setEmergencyHotline("0911 222 333 (24/7)");

        mockMvc.perform(put("/api/cms/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clinicName").value("DentalCare Luxury Command Center Premium"))
                .andExpect(jsonPath("$.data.hotline").value("1800 9999"));

        // Verify changes are retrieved on subsequent GET
        mockMvc.perform(get("/api/cms/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clinicName").value("DentalCare Luxury Command Center Premium"));

        // Reset back to default
        mockMvc.perform(post("/api/cms/config/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clinicName").value("DentalCare Luxury Dental Clinic"));
    }

    @Test
    @DisplayName("3. Export Excel/CSV: Xuất Lịch Hẹn Khám (Appointments) chuẩn UTF-8 BOM")
    void testExportAppointmentsCsv() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/excel?type=appointments"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-lich-kham-")))
                .andReturn();

        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertTrue(contentBytes.length >= 3, "CSV stream must not be empty");

        // Verify UTF-8 BOM (\uFEFF) -> 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, contentBytes[0], "First byte must be UTF-8 BOM 0xEF");
        assertEquals((byte) 0xBB, contentBytes[1], "Second byte must be UTF-8 BOM 0xBB");
        assertEquals((byte) 0xBF, contentBytes[2], "Third byte must be UTF-8 BOM 0xBF");

        String csvString = new String(contentBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.startsWith("\uFEFFMã Lịch Hẹn,Ngày Khám,Giờ Khám"), "Header must match appointment export schema");
        assertTrue(csvString.contains("Bác Sĩ Phụ Trách"), "Header must include dentist column");
    }

    @Test
    @DisplayName("4. Export Excel/CSV: Xuất Tồn Kho Vật Tư (Inventory) chuẩn UTF-8 BOM")
    void testExportInventoryCsv() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/excel?type=inventory"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-ton-kho-vat-tu-")))
                .andReturn();

        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertTrue(contentBytes.length >= 3);

        // Verify UTF-8 BOM
        assertEquals((byte) 0xEF, contentBytes[0]);
        assertEquals((byte) 0xBB, contentBytes[1]);
        assertEquals((byte) 0xBF, contentBytes[2]);

        String csvString = new String(contentBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("Mã Sản Phẩm / Vật Tư"), "Header must contain product code");
        assertTrue(csvString.contains("Định Giá Tồn Kho (VNĐ)"), "Header must contain inventory valuation");
        assertTrue(csvString.contains("Trạng Thái Tồn Kho"), "Header must contain safety status");
    }

    @Test
    @DisplayName("5. Export Excel/CSV: Xuất Báo Cáo Hiệu Suất & KPI Bác Sĩ chuẩn UTF-8 BOM")
    void testExportDoctorKpiCsv() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/excel?type=kpi"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-hieu-suat-kpi-bac-si-")))
                .andReturn();

        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertTrue(contentBytes.length >= 3);

        assertEquals((byte) 0xEF, contentBytes[0]);
        assertEquals((byte) 0xBB, contentBytes[1]);
        assertEquals((byte) 0xBF, contentBytes[2]);

        String csvString = new String(contentBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("Mã Bác Sĩ"), "Header must contain doctor ID");
        assertTrue(csvString.contains("Họ Và Tên Bác Sĩ"), "Header must contain doctor full name");
        assertTrue(csvString.contains("Tỷ Lệ Hoàn Thành (%)"), "Header must contain completion rate");
        assertTrue(csvString.contains("Đánh Giá Hiệu Suất"), "Header must contain KPI performance evaluation");
    }

    @Test
    @DisplayName("6. Export Excel/CSV: Xuất Báo Cáo Tiếp Nhận Hiện Trường / Leads chuẩn UTF-8 BOM")
    void testExportFieldIntakeCsv() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/excel?type=intake"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-tiep-nhan-hien-truong-")))
                .andReturn();

        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertTrue(contentBytes.length >= 3);

        assertEquals((byte) 0xEF, contentBytes[0]);
        assertEquals((byte) 0xBB, contentBytes[1]);
        assertEquals((byte) 0xBF, contentBytes[2]);

        String csvString = new String(contentBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("Mã Lead / Hồ Sơ"), "Header must contain lead ID");
        assertTrue(csvString.contains("Họ Tên Bệnh Nhân / Học Sinh"), "Header must contain lead name");
        assertTrue(csvString.contains("Khuyến Nghị Điều Trị"), "Header must contain treatment recommendation");
        assertTrue(csvString.contains("Trạng Thái Lead"), "Header must contain lead status");

        // Verify alias type=leads also works identically
        mockMvc.perform(get("/api/export/excel?type=leads"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
    }

    @Test
    @DisplayName("7. Export Excel/CSV: Mặc định không truyền type trả về Appointments CSV")
    void testExportDefaultType() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/export/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("bao-cao-lich-kham-")))
                .andReturn();

        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertEquals((byte) 0xEF, contentBytes[0]);
        assertEquals((byte) 0xBB, contentBytes[1]);
        assertEquals((byte) 0xBF, contentBytes[2]);
    }
}
