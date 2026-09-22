package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.CreateForumPostRequestDto;
import com.dentalclinic.model.CommunityPostCategory;
import com.dentalclinic.model.DentalCommunityPost;
import com.dentalclinic.service.DentalCommunityPostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class DentalCommunityPostController {

    private final DentalCommunityPostService communityPostService;

    public DentalCommunityPostController(DentalCommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<DentalCommunityPost>> getPosts(@RequestParam(required = false) CommunityPostCategory category) {
        if (category != null) {
            return ApiResponse.success(communityPostService.getPostsByCategory(category));
        }
        return ApiResponse.success(communityPostService.getAllApprovedPosts());
    }

    @PostMapping("/posts")
    public ApiResponse<DentalCommunityPost> createPost(@RequestBody CreateForumPostRequestDto request) {
        DentalCommunityPost post = communityPostService.createPost(request, null);
        return ApiResponse.success("Đăng bài viết thành công!", post);
    }

    @PostMapping("/posts/{id}/like")
    public ApiResponse<DentalCommunityPost> likePost(@PathVariable Long id) {
        return ApiResponse.success("Đã thích bài viết", communityPostService.likePost(id));
    }
}
