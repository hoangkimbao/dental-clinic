package com.dentalclinic.repository;

import com.dentalclinic.model.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByPatientId(Long patientId);
    Optional<LoyaltyAccount> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
