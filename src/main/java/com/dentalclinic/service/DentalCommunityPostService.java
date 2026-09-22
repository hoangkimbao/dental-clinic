package com.dentalclinic.service;

import com.dentalclinic.dto.CreateForumPostRequestDto;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.CommunityPostCategory;
import com.dentalclinic.model.DentalCommunityPost;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.DentalCommunityPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DentalCommunityPostService {

    private final DentalCommunityPostRepository postRepository;

    public DentalCommunityPostService(DentalCommunityPostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<DentalCommunityPost> getAllApprovedPosts() {
        return postRepository.findByApprovedTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<DentalCommunityPost> getPostsByCategory(CommunityPostCategory category) {
        return postRepository.findByCategoryAndApprovedTrueOrderByCreatedAtDesc(category);
    }

    public DentalCommunityPost createPost(CreateForumPostRequestDto dto, User author) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Tiêu đề bài viết không được để trống!");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new BadRequestException("Nội dung bài viết không được để trống!");
        }

        String authorName = (author != null && author.getFullName() != null)
                ? author.getFullName()
                : (dto.getAuthorName() != null ? dto.getAuthorName().trim() : "Khách ẩn danh");

        String authorPhone = (author != null && author.getPhone() != null)
                ? author.getPhone()
                : (dto.getAuthorPhone() != null ? dto.getAuthorPhone().trim() : null);

        DentalCommunityPost post = new DentalCommunityPost(
                author,
                authorName,
                authorPhone,
                dto.getTitle().trim(),
                dto.getContent().trim(),
                dto.getCategory() != null ? dto.getCategory() : CommunityPostCategory.EXPERIENCE,
                0,
                true // Approved by default for friendly community sharing
        );

        return postRepository.save(post);
    }

    public DentalCommunityPost likePost(Long id) {
        DentalCommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bài viết không tồn tại với ID: " + id));
        post.setLikesCount(post.getLikesCount() + 1);
        return postRepository.save(post);
    }
}
