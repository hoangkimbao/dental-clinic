package com.dentalclinic.repository;

import com.dentalclinic.model.ClinicBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicBranchRepository extends JpaRepository<ClinicBranch, Long> {
    Optional<ClinicBranch> findByCode(String code);
    List<ClinicBranch> findByCity(String city);
    boolean existsByCode(String code);
}
