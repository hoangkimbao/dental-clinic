package com.dentalclinic.service;

import com.dentalclinic.dto.FieldIntakeRequest;
import com.dentalclinic.dto.UpdateLeadStatusRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.ClinicBranchRepository;
import com.dentalclinic.repository.FieldPatientIntakeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FieldPatientIntakeService {

    private final FieldPatientIntakeRepository intakeRepository;
    private final ClinicBranchRepository branchRepository;

    public FieldPatientIntakeService(FieldPatientIntakeRepository intakeRepository,
                                     ClinicBranchRepository branchRepository) {
        this.intakeRepository = intakeRepository;
        this.branchRepository = branchRepository;
    }

    public FieldPatientIntake captureLead(FieldIntakeRequest req, User currentUser) {
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Số điện thoại bệnh nhân/phụ huynh không được để trống!");
        }

        String fullName = req.getPatientFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = (req.getPatientName() != null && !req.getPatientName().trim().isEmpty())
                    ? req.getPatientName().trim() : "Bệnh nhân hiện trường";
        }

        String leadCode = "FLD-" + LocalDate.now().getYear() + "-" + String.format("%04d", (intakeRepository.count() + 1));

        FieldPatientIntake lead = new FieldPatientIntake();
        lead.setLeadCode(leadCode);
        lead.setEventName(req.getEventName() != null ? req.getEventName() : "Sự Kiện Khám Răng Cộng Đồng");
        lead.setEventType(req.getEventType() != null ? req.getEventType() : FieldEventType.SCHOOL_SCREENING);
        lead.setEventLocation(req.getEventLocation());
        lead.setEventDate(LocalDate.now());
        lead.setPatientFullName(fullName);
        lead.setPatientName(fullName);
        lead.setBirthYear(req.getBirthYear());
        lead.setStudentClass(req.getStudentClass());
        lead.setPhone(req.getPhone().trim());
        lead.setEmail(req.getEmail());
        lead.setParentName(req.getParentName());
        lead.setParentPhone(req.getParentPhone());
        lead.setScreeningFindings(req.getScreeningFindings());
        lead.setInitialComplaint(req.getScreeningFindings());
        lead.setRecommendation(req.getRecommendation());
        lead.setPreliminaryPathology(req.getRecommendation());
        lead.setVoucherCode(req.getVoucherCode());
        lead.setCapturedBy(currentUser);
        lead.setStatus(FieldLeadStatus.NEW);
        lead.setNotes(req.getNotes());

        if (req.getBranchId() != null) {
            branchRepository.findById(req.getBranchId()).ifPresent(lead::setAssignedBranch);
        }

        return intakeRepository.save(lead);
    }

    @Transactional(readOnly = true)
    public List<FieldPatientIntake> getAllLeads(String eventName) {
        if (eventName != null && !eventName.trim().isEmpty()) {
            return intakeRepository.findByEventNameContainingIgnoreCaseOrderByCreatedAtDesc(eventName.trim());
        }
        return intakeRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public FieldPatientIntake getLeadById(Long id) {
        return intakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tiếp nhận hiện trường với ID: " + id));
    }

    public FieldPatientIntake updateLeadStatus(Long id, UpdateLeadStatusRequest req) {
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            throw new BadRequestException("Trạng thái cập nhật không được để trống!");
        }

        FieldPatientIntake lead = getLeadById(id);
        lead.setStatus(FieldLeadStatus.fromString(req.getStatus()));

        if (req.getNotes() != null && !req.getNotes().trim().isEmpty()) {
            String combined = (lead.getNotes() != null && !lead.getNotes().isEmpty())
                    ? lead.getNotes() + " | " + req.getNotes()
                    : req.getNotes();
            lead.setNotes(combined);
        }

        return intakeRepository.save(lead);
    }

    public List<FieldPatientIntake> batchSyncLeads(List<FieldIntakeRequest> requests, User currentUser) {
        List<FieldPatientIntake> savedLeads = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return savedLeads;
        }

        for (FieldIntakeRequest req : requests) {
            if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
                req.setPhone("0900000000"); // Fallback for batch sync if phone missing
            }
            savedLeads.add(captureLead(req, currentUser));
        }
        return savedLeads;
    }
}
