package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityCommentsController {

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<Object> addComment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        // Mock successful comment creation
        return ApiResponse.success("Đã thêm bình luận thành công", Map.of(
                "id", 1,
                "postId", id,
                "content", request.get("content"),
                "createdAt", java.time.LocalDateTime.now().toString()
        ));
    }
}
