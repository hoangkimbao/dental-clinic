package com.dentalclinic.controller;

import com.dentalclinic.model.DoctorReview;
import com.dentalclinic.service.DoctorReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class DoctorReviewController {

    @Autowired
    private DoctorReviewService reviewService;

    @GetMapping("/dentist/{dentistId}")
    public ResponseEntity<List<DoctorReview>> getReviewsByDentist(@PathVariable Long dentistId) {
        return ResponseEntity.ok(reviewService.getReviewsByDentist(dentistId));
    }

    @GetMapping("/stats/{dentistId}")
    public ResponseEntity<Map<String, Object>> getDentistStats(@PathVariable Long dentistId) {
        return ResponseEntity.ok(reviewService.getDentistReviewStats(dentistId));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<DoctorReview>> getLatestReviews() {
        return ResponseEntity.ok(reviewService.getLatestReviews());
    }

    @PostMapping
    public ResponseEntity<DoctorReview> createReview(@RequestBody DoctorReview review) {
        return ResponseEntity.ok(reviewService.createReview(review));
    }
}
