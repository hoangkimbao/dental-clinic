package com.dentalclinic.repository;

import com.dentalclinic.model.CommunityPostCategory;
import com.dentalclinic.model.DentalCommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DentalCommunityPostRepository extends JpaRepository<DentalCommunityPost, Long> {
    List<DentalCommunityPost> findByApprovedTrueOrderByCreatedAtDesc();
    List<DentalCommunityPost> findByCategoryAndApprovedTrueOrderByCreatedAtDesc(CommunityPostCategory category);
}
