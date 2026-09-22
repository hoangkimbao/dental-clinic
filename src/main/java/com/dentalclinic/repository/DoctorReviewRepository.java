package com.dentalclinic.repository;

import com.dentalclinic.model.DoctorReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {
    List<DoctorReview> findByDentistIdOrderByCreatedAtDesc(Long dentistId);
    List<DoctorReview> findTop10ByOrderByCreatedAtDesc();
}
