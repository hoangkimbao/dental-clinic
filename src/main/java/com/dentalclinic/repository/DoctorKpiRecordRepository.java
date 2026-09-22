package com.dentalclinic.repository;

import com.dentalclinic.model.DoctorKpiRecord;
import com.dentalclinic.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorKpiRecordRepository extends JpaRepository<DoctorKpiRecord, Long> {
    Optional<DoctorKpiRecord> findByDoctorAndPeriodMonth(User doctor, String periodMonth);
    Optional<DoctorKpiRecord> findByDoctor_IdAndPeriodMonth(Long doctorId, String periodMonth);
    List<DoctorKpiRecord> findByDoctorOrderByRecordDateDesc(User doctor);
    List<DoctorKpiRecord> findByDoctor_IdOrderByRecordDateDesc(Long doctorId);
    List<DoctorKpiRecord> findByPeriodMonthOrderByKpiScoreDesc(String periodMonth);
    List<DoctorKpiRecord> findAllByOrderByKpiScoreDesc();
}
