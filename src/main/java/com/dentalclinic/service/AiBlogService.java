package com.dentalclinic.service;

import com.dentalclinic.model.Article;
import com.dentalclinic.repository.ArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiBlogService {

    private static final Logger log = LoggerFactory.getLogger(AiBlogService.class);

    @Value("${ninerouter.url:http://localhost:20128}")
    private String nineRouterUrl;

    @Value("${ninerouter.key:}")
    private String nineRouterKey;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public Article generateArticleFromAi(String topic, String requestedCategory, String author) throws Exception {
        String apiKey = (nineRouterKey != null && !nineRouterKey.isBlank())
                ? nineRouterKey
                : System.getenv("NINEROUTER_KEY");

        String prompt = "Bạn là Bác sĩ Chuyên khoa Răng Hàm Mặt hàng đầu và Chuyên gia Sáng tạo Nội dung Y Khoa chuẩn SEO Google (chuẩn E-E-A-T).\n" +
                "Chủ đề cần viết: \"" + topic + "\"\n" +
                (requestedCategory != null && !requestedCategory.isBlank() ? "Chuyên mục ưu tiên: " + requestedCategory + "\n" : "") +
                (author != null && !author.isBlank() ? "Bác sĩ tham vấn: " + author + "\n" : "") +
                "Hãy biên soạn một bài viết cẩm nang y khoa nha khoa chuyên sâu, văn phong y khoa ấm áp, chuẩn xác, hấp dẫn, bố cục đẹp mắt.\n" +
                "Yêu cầu trả về ĐÚNG ĐỊNH DẠNG JSON THUẦN TÚY (không bọc trong markdown code fence, không có chữ thừa trước/sau):\n" +
                "{\n" +
                "  \"title\": \"Tiêu đề chuẩn SEO, khoa học, dưới 70 ký tự\",\n" +
                "  \"slug\": \"url-slug-khong-dau-ngan-gon\",\n" +
                "  \"category\": \"NIENG_RANG hoặc IMPLANT hoặc RANG_SU hoặc RANG_KHON hoặc NHA_KHOA_TRE_EM hoặc BENH_LY_MIENG\",\n" +
                "  \"authorName\": \"" + (author != null && !author.isBlank() ? author : "BS.CKII Trần Văn Thắng (Chuyên Gia Hàm Mặt & Implant)") + "\",\n" +
                "  \"authorAvatar\": \"https://images.unsplash.com/photo-1622253692010-333f2da6031d?q=80&w=400&auto=format&fit=crop\",\n" +
                "  \"readTime\": \"5 phút đọc\",\n" +
                "  \"icon\": \"fa-tooth\",\n" +
                "  \"summary\": \"Đoạn meta description tóm tắt 2-3 câu nêu bật giải pháp và lợi ích cho bệnh nhân\",\n" +
                "  \"content\": \"<p>Mở đầu thu hút về thực trạng vấn đề...</p><h2>1. Nguyên Nhân & Chỉ Định Y Khoa</h2><p>Phân tích chuyên sâu...</p><h2>2. Quy Trình Điều Trị Chuẩn Y Tế Tại DentalCare</h2><ul><li><b>Bước 1:</b> Thăm khám & chụp CT 3D Cone Beam miễn phí</li><li><b>Bước 2:</b> Lên phác đồ kỹ thuật số...</li></ul><h2>3. Lời Khuyên Của Bác Sĩ Chuyên Khoa</h2><div style='background:#f0fdf4; border-left:4px solid #10b981; padding:15px; border-radius:8px;'><b>Lưu ý từ Bác sĩ:</b> Lời khuyên quý giá...</div>\"\n" +
                "}";

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("model", "fast-combo");
        reqBody.put("messages", List.of(
                Map.of("role", "system", "content", "Bạn là trợ lý AI chuyên tạo bài viết y khoa JSON cho hệ thống nha khoa."),
                Map.of("role", "user", "content", prompt)
        ));
        reqBody.put("temperature", 0.7);
        reqBody.put("stream", false);

        String jsonPayload = objectMapper.writeValueAsString(reqBody);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(nineRouterUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        log.info("[AiBlogService] Requesting 9Router to generate article for topic: {}", topic);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[AiBlogService] 9Router error: status {}, body: {}", response.statusCode(), response.body());
            throw new RuntimeException("9Router API error (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String rawContent = root.path("choices").get(0).path("message").path("content").asText();

        String cleaned = rawContent.replaceAll("(?i)```json", "").replaceAll("```", "").trim();

        JsonNode articleJson = objectMapper.readTree(cleaned);

        String title = articleJson.path("title").asText(topic);
        String baseSlug = articleJson.path("slug").asText().trim();
        if (baseSlug.isEmpty()) {
            baseSlug = "bai-viet-" + System.currentTimeMillis();
        }

        String slug = baseSlug;
        int count = 1;
        while (articleRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + (++count);
        }

        String category = articleJson.path("category").asText(requestedCategory != null ? requestedCategory : "NIENG_RANG");
        String authorName = articleJson.path("authorName").asText("BS.CKII Trần Văn Thắng");
        String authorAvatar = articleJson.path("authorAvatar").asText("https://images.unsplash.com/photo-1622253692010-333f2da6031d?q=80&w=400&auto=format&fit=crop");
        String readTime = articleJson.path("readTime").asText("5 phút đọc");
        String icon = articleJson.path("icon").asText("fa-tooth");
        String summary = articleJson.path("summary").asText();
        String content = articleJson.path("content").asText();

        Article article = new Article();
        article.setTitle(title);
        article.setSlug(slug);
        article.setCategory(category);
        article.setAuthorName(authorName);
        article.setAuthorAvatar(authorAvatar);
        article.setReadTime(readTime);
        article.setIcon(icon);
        article.setSummary(summary);
        article.setContent(content);
        article.setViewCount(0);
        article.setIsPublished(true);

        return article;
    }

    public Article generateAndPublish(String topic, String category, String author) throws Exception {
        Article article = generateArticleFromAi(topic, category, author);
        return articleRepository.save(article);
    }
}