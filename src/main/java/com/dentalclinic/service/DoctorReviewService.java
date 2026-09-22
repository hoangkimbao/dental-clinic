package com.dentalclinic.service;

import com.dentalclinic.model.DoctorReview;
import com.dentalclinic.repository.DoctorReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorReviewService {

    @Autowired
    private DoctorReviewRepository reviewRepository;

    public List<DoctorReview> getReviewsByDentist(Long dentistId) {
        return reviewRepository.findByDentistIdOrderByCreatedAtDesc(dentistId);
    }

    public List<DoctorReview> getLatestReviews() {
        return reviewRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public DoctorReview createReview(DoctorReview review) {
        if (review.getRating() == null || review.getRating() < 1) review.setRating(5);
        if (review.getRating() > 5) review.setRating(5);
        if (review.getVerifiedBadge() == null) review.setVerifiedBadge("Khách hàng đã khám thực tế");
        return reviewRepository.save(review);
    }

    public Map<String, Object> getDentistReviewStats(Long dentistId) {
        List<DoctorReview> reviews = reviewRepository.findByDentistIdOrderByCreatedAtDesc(dentistId);
        double avg = reviews.isEmpty() ? 5.0 : reviews.stream().mapToInt(DoctorReview::getRating).average().orElse(5.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("dentistId", dentistId);
        stats.put("totalReviews", reviews.size());
        stats.put("averageRating", Math.round(avg * 10.0) / 10.0);
        stats.put("reviews", reviews);
        return stats;
    }
}
