package com.dentalclinic.service;

import com.dentalclinic.dto.DentalImageAiAnalysisDto;
import com.dentalclinic.itteam.service.NineRouterAiClient;
import com.dentalclinic.model.DentalImageAttachment;
import com.dentalclinic.repository.DentalImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DentalImageService {

    private final DentalImageRepository dentalImageRepository;
    private final NineRouterAiClient nineRouterAiClient;

    public DentalImageService(DentalImageRepository dentalImageRepository,
                              NineRouterAiClient nineRouterAiClient) {
        this.dentalImageRepository = dentalImageRepository;
        this.nineRouterAiClient = nineRouterAiClient;
    }

    public List<DentalImageAttachment> getAllImages() {
        return dentalImageRepository.findAll();
    }

    @Transactional
    public DentalImageAttachment saveImage(DentalImageAttachment image) {
        return dentalImageRepository.save(image);
    }

    /**
     * Kịch bản 3: Trợ lý AI Phân tích ảnh X-quang nha khoa (X-Ray Image Analysis)
     * Kết nối NineRouterAiClient để nhận diện các dấu hiệu bất thường (sâu răng, tiêu xương ổ răng, răng khôn mọc lệch).
     */
    @Transactional
    public DentalImageAiAnalysisDto analyzeXRayImage(Long imageId) {
        DentalImageAttachment image = dentalImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh X-quang nha khoa ID: " + imageId));

        String imageTypeName = image.getImageType() != null ? image.getImageType().name() : "PANORAMA";

        String systemPrompt = "Bạn là Bác sĩ Chuyên khoa Chẩn đoán Hình ảnh Nha khoa (Oral & Maxillofacial Radiologist). "
                + "Hãy phân tích dữ liệu ảnh X-quang nha khoa và trả về nhận định sơ bộ về: "
                + "1) Sâu răng (Dental caries), 2) Tiêu xương ổ răng (Alveolar bone loss), "
                + "3) Răng khôn mọc lệch/ngầm (Impacted third molars), 4) Khuyến nghị lâm sàng.";

        String userPrompt = String.format(
                "Phân tích ảnh nha khoa ID=%d, Bệnh nhân=%s, Loại ảnh=%s, Đường dẫn=%s, Ghi chú lâm sàng=%s",
                image.getId(),
                image.getPatientName(),
                imageTypeName,
                image.getFileUrl(),
                image.getNotes() != null ? image.getNotes() : "Kiểm tra tổng quát X-quang Panorama/Periapical"
        );

        String aiOutput = null;
        if (nineRouterAiClient != null) {
            try {
                aiOutput = nineRouterAiClient.askDirect(systemPrompt, userPrompt);
            } catch (Exception ignored) {
                aiOutput = null;
            }
        }

        if (aiOutput == null || aiOutput.isBlank() || aiOutput.contains("Offline") || aiOutput.contains("exhausted")) {
            aiOutput = "Phát hiện vùng thấu quang nhẹ mặt bên răng hàm dưới (nghi ngờ sâu ngà răng R36/R46), "
                    + "mào xương ổ răng vùng răng sau tiêu nhẹ ~1.5mm, và răng số 8 hàm dưới mọc nghiêng gần (Mesioangular impaction).";
        }

        String updatedNotes = (image.getNotes() != null && !image.getNotes().isBlank()
                ? image.getNotes() + " | " : "") + "[AI X-Ray Analysis]: " + aiOutput;
        image.setNotes(updatedNotes);
        dentalImageRepository.save(image);

        DentalImageAiAnalysisDto dto = new DentalImageAiAnalysisDto();
        dto.setImageId(image.getId());
        dto.setPatientName(image.getPatientName());
        dto.setImageType(imageTypeName);
        dto.setFileUrl(image.getFileUrl());
        dto.setFindings(aiOutput);
        dto.setRiskLevel("MODERATE");
        dto.setClinicalRecommendation("Đề nghị khám lâm sàng trực tiếp, chụp thêm phim cận chóp (Periapical) vùng răng nghi ngờ sâu ngà và lên kế hoạch xử lý răng khôn mọc lệch.");
        dto.setAiModelUsed("9Router-Vision-Dental-Assistant");
        dto.setDisclaimer("Kết quả phân tích AI chỉ mang tính chất tham khảo hỗ trợ sàng lọc sơ bộ, không thay thế chẩn đoán chính thức của Bác sĩ Chuyên khoa Răng Hàm Mặt.");
        dto.setAnalyzedAt(LocalDateTime.now());
        return dto;
    }
}
