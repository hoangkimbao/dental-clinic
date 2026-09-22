package com.dentalclinic;

import com.dentalclinic.dto.AiDiagnosticRequestDto;
import com.dentalclinic.dto.CreateForumPostRequestDto;
import com.dentalclinic.dto.OrderItemRequestDto;
import com.dentalclinic.dto.OrderRequestDto;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerEcosystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DentalServiceCatalogRepository serviceRepository;

    @Autowired
    private DentalProductRepository productRepository;

    @Autowired
    private PorcelainCrownWarrantyRepository warrantyRepository;

    @Autowired
    private ClinicBranchRepository branchRepository;

    @Autowired
    private DentalCommunityPostRepository forumRepository;

    @Autowired
    private LoyaltyAccountRepository loyaltyRepository;

    @Test
    @DisplayName("1. Catalog: Tra cứu danh mục dịch vụ nha khoa trực tuyến")
    void testDentalServicesCatalog() throws Exception {
        mockMvc.perform(get("/api/dental-services")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.data[0].code").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].price").exists());

        // Test category filter
        mockMvc.perform(get("/api/dental-services?category=ORTHODONTICS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].category").value("ORTHODONTICS"));

        // Test featured services
        mockMvc.perform(get("/api/dental-services/featured")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("2. Shop: Danh sách sản phẩm kèm quy cách đóng gói Hộp đơn & Combo")
    void testDentalProductsAndPackaging() throws Exception {
        mockMvc.perform(get("/api/dental-products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.data[0].packagingOptions", hasSize(greaterThanOrEqualTo(1))));

        // Filter by category
        mockMvc.perform(get("/api/dental-products?category=BRUSH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].category").value("BRUSH"));
    }

    @Test
    @DisplayName("3. Orders: Khởi tạo đơn hàng sản phẩm nha khoa và tích lũy điểm Loyalty")
    void testCreateDentalOrderWithLoyalty() throws Exception {
        DentalProduct product = productRepository.findAll().get(0);
        assertNotNull(product);

        ProductPackagingOption packagingOption = product.getPackagingOptions().isEmpty() 
                ? null 
                : product.getPackagingOptions().get(0);

        OrderRequestDto orderDto = new OrderRequestDto();
        orderDto.setCustomerName("Trần Thị Mai");
        orderDto.setCustomerPhone("0988776655");
        orderDto.setShippingAddress("Số 15 Lê Duẩn, Hoàn Kiếm, Hà Nội");
        orderDto.setPaymentMethod("COD");

        OrderItemRequestDto itemDto = new OrderItemRequestDto();
        itemDto.setProductId(product.getId());
        itemDto.setPackagingOptionId(packagingOption != null ? packagingOption.getId() : null);
        itemDto.setQuantity(2);
        orderDto.setItems(List.of(itemDto));

        mockMvc.perform(post("/api/dental-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderCode").exists())
                .andExpect(jsonPath("$.data.customerName").value("Trần Thị Mai"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount", greaterThan(0.0)))
                .andExpect(jsonPath("$.data.loyaltyPointsEarned", greaterThan(0)));

        // Verify loyalty account exists and accumulated points
        LoyaltyAccount loyalty = loyaltyRepository.findByPhoneNumber("0988776655").orElse(null);
        assertNotNull(loyalty);
        assertTrue(loyalty.getLoyaltyPoints() > 0);
    }

    @Test
    @DisplayName("4. Warranty: Tra cứu thẻ bảo hành răng sứ điện tử và QR Code")
    void testPorcelainCrownWarrantyLookup() throws Exception {
        // Query by serial code
        mockMvc.perform(get("/api/warranties/lookup?query=DC-WR-2026-88992")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serialCode").value("DC-WR-2026-88992"))
                .andExpect(jsonPath("$.data.crownType").value("LAVA_PLUS"))
                .andExpect(jsonPath("$.data.warrantyYears").value(15))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Query by QR code
        mockMvc.perform(get("/api/warranties/lookup?query=DENTALCARE-CERCON-77123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.crownType").value("CERCON_HT"));

        // Verify QR direct endpoint
        mockMvc.perform(get("/api/warranties/verify-qr?qrCode=DENTALCARE-CERCON-77123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.serialCode").value("DC-WR-2026-77123"));

        // Invalid query returns 404
        mockMvc.perform(get("/api/warranties/lookup?query=NON_EXISTENT_SERIAL_999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("5. AI Diagnostic: Chẩn đoán bệnh lý răng miệng thông minh")
    void testAiDentalDiagnostic() throws Exception {
        // Test Wisdom tooth condition
        AiDiagnosticRequestDto req1 = new AiDiagnosticRequestDto();
        req1.setPatientName("Lê Văn Tùng");
        req1.setPatientPhone("0912999888");
        req1.setSymptomsDescription("Đau nhức góc hàm dưới dữ dội, mọc răng khôn đâm vào má");
        req1.setModelUsed("9router/gemini-2.5-flash");

        mockMvc.perform(post("/api/dental-ai/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pathology").value("IMPACTED_WISDOM"))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.treatmentAdvice").exists())
                .andExpect(jsonPath("$.data.estimatedCostRange").exists());

        // Test Caries condition
        AiDiagnosticRequestDto req2 = new AiDiagnosticRequestDto();
        req2.setPatientName("Phạm Thu Hà");
        req2.setSymptomsDescription("Lỗ sâu men răng có vết đen, ê buốt khi ăn đồ ngọt");

        mockMvc.perform(post("/api/dental-ai/diagnose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pathology").value("CARIES"));
    }

    @Test
    @DisplayName("6. Branches: Định vị chi nhánh gần nhất bằng GPS Haversine")
    void testClinicBranchesAndGpsNearest() throws Exception {
        // List all branches
        mockMvc.perform(get("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(4))));

        // Find nearest branch near Ho Chi Minh City (10.76, 106.60)
        mockMvc.perform(get("/api/branches/nearest?latitude=10.76&longitude=106.60")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branch.city").value("TP. Hồ Chí Minh"))
                .andExpect(jsonPath("$.data.distanceKm", lessThan(30.0)));

        // Find nearest branch near Hanoi (21.02, 105.85)
        mockMvc.perform(get("/api/branches/nearest?latitude=21.02&longitude=105.85")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branch.city").value("Hà Nội"))
                .andExpect(jsonPath("$.data.distanceKm", lessThan(30.0)));
    }

    @Test
    @DisplayName("7. Forum: Đăng bài hỏi đáp và thích (Like) bài viết cộng đồng")
    void testDentalCommunityForum() throws Exception {
        // Create forum post
        CreateForumPostRequestDto postDto = new CreateForumPostRequestDto();
        postDto.setAuthorName("Bệnh nhân Đặng Hoàng");
        postDto.setAuthorPhone("0933112233");
        postDto.setCategory(CommunityPostCategory.EXPERIENCE);
        postDto.setTitle("Trải nghiệm niềng răng trong suốt 6 tháng tại cơ sở Bình Tân");
        postDto.setContent("Quá trình chỉnh nha rất êm ái, bác sĩ Thắng theo dõi sát sao từng khay niềng!");

        String responseStr = mockMvc.perform(post("/api/forum/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.title").value(postDto.getTitle()))
                .andReturn().getResponse().getContentAsString();

        Long createdPostId = objectMapper.readTree(responseStr).path("data").path("id").asLong();

        // Like the forum post
        mockMvc.perform(post("/api/forum/posts/" + createdPostId + "/like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.likesCount", greaterThanOrEqualTo(1)));

        // Filter forum posts
        mockMvc.perform(get("/api/forum/posts?category=EXPERIENCE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}
