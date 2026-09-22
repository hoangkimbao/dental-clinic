package com.dentalclinic.controller;

import com.dentalclinic.model.Article;
import com.dentalclinic.service.AiBlogService;
import com.dentalclinic.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private AiBlogService aiBlogService;

    @GetMapping
    public ResponseEntity<List<Article>> getAllArticles(@RequestParam(required = false) String category) {
        if (category != null && !category.trim().isEmpty()) {
            return ResponseEntity.ok(articleService.getArticlesByCategory(category));
        }
        return ResponseEntity.ok(articleService.getAllPublishedArticles());
    }

    @GetMapping("/throw-simulated-error")
    public ResponseEntity<?> throwSimulatedError() {
        throw new RuntimeException("Simulated unexpected internal error for security testing");
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Article> getArticleBySlug(@PathVariable String slug) {
        return articleService.getArticleBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Article> createArticle(@RequestBody Article article) {
        return ResponseEntity.ok(articleService.createArticle(article));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        return articleService.updateArticle(id, article)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai-generate")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> aiGenerate(@RequestBody Map<String, String> payload) {
        String topic = payload.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập chủ đề bài viết cần viết."));
        }
        String category = payload.get("category");
        String author = payload.get("author");
        try {
            Article generated = aiBlogService.generateArticleFromAi(topic, category, author);
            return ResponseEntity.ok(generated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi sinh bài viết 9Router: " + e.getMessage()));
        }
    }

    @PostMapping("/ai-generate-and-publish")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> aiGenerateAndPublish(@RequestBody Map<String, String> payload) {
        String topic = payload.get("topic");
        if (topic == null || topic.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập chủ đề bài viết cần viết."));
        }
        String category = payload.get("category");
        String author = payload.get("author");
        try {
            Article published = aiBlogService.generateAndPublish(topic, category, author);
            return ResponseEntity.ok(published);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi sinh bài viết 9Router: " + e.getMessage()));
        }
    }

    @PostMapping("/ai-batch-generate")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> aiBatchGenerate(@RequestBody Map<String, Object> payload) {
        Object topicsObj = payload.get("topics");
        if (!(topicsObj instanceof List)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Danh sách topics không hợp lệ."));
        }
        List<?> topics = (List<?>) topicsObj;
        List<Article> publishedList = new ArrayList<>();
        for (Object t : topics) {
            if (t != null && !t.toString().trim().isEmpty()) {
                try {
                    Article art = aiBlogService.generateAndPublish(t.toString().trim(), null, null);
                    publishedList.add(art);
                } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok(publishedList);
    }
}