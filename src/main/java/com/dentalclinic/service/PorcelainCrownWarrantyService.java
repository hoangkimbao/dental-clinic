package com.dentalclinic.service;

import com.dentalclinic.dto.WarrantyLookupResponseDto;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.CrownType;
import com.dentalclinic.model.PorcelainCrownWarranty;
import com.dentalclinic.model.WarrantyStatus;
import com.dentalclinic.repository.PorcelainCrownWarrantyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PorcelainCrownWarrantyService {

    private final PorcelainCrownWarrantyRepository warrantyRepository;

    public PorcelainCrownWarrantyService(PorcelainCrownWarrantyRepository warrantyRepository) {
        this.warrantyRepository = warrantyRepository;
    }

    @Transactional(readOnly = true)
    public WarrantyLookupResponseDto lookupByCodeOrQr(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResourceNotFoundException("Vui lòng cung cấp mã thẻ bảo hành hoặc mã QR!");
        }

        String clean = query.trim();
        Optional<PorcelainCrownWarranty> opt = warrantyRepository.findByWarrantyCode(clean);
        if (opt.isEmpty()) {
            opt = warrantyRepository.findByQrCodeString(clean);
        }

        PorcelainCrownWarranty warranty = opt.orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy thẻ bảo hành hợp lệ với mã: " + clean));

        return toDto(warranty);
    }

    @Transactional(readOnly = true)
    public List<PorcelainCrownWarranty> getWarrantiesByPhone(String phone) {
        return warrantyRepository.findByPatientPhone(phone);
    }

    public PorcelainCrownWarranty registerWarranty(PorcelainCrownWarranty warranty) {
        if (warranty.getStartDate() == null) {
            warranty.setStartDate(LocalDate.now());
        }
        if (warranty.getEndDate() == null && warranty.getWarrantyYears() != null) {
            warranty.setEndDate(warranty.getStartDate().plusYears(warranty.getWarrantyYears()));
        }
        return warrantyRepository.save(warranty);
    }

    private WarrantyLookupResponseDto toDto(PorcelainCrownWarranty w) {
        WarrantyLookupResponseDto dto = new WarrantyLookupResponseDto();
        dto.setWarrantyCode(w.getWarrantyCode());
        dto.setPatientName(w.getPatientName());
        dto.setPatientPhoneMasked(maskPhone(w.getPatientPhone()));
        dto.setCrownType(w.getCrownType());
        dto.setCrownTypeName(getCrownDisplayName(w.getCrownType()));
        dto.setToothPositions(w.getToothPositions());
        dto.setLaboOrigin(w.getLaboOrigin());
        dto.setWarrantyYears(w.getWarrantyYears());
        dto.setStartDate(w.getStartDate());
        dto.setEndDate(w.getEndDate());
        dto.setQrCodeString(w.getQrCodeString());
        dto.setStatus(w.getStatus());

        LocalDate now = LocalDate.now();
        boolean isValid = (w.getStatus() == WarrantyStatus.ACTIVE) &&
                (w.getEndDate() == null || !now.isAfter(w.getEndDate()));
        dto.setValid(isValid);

        if (w.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(now, w.getEndDate());
            dto.setRemainingDays(Math.max(0, days));
        } else {
            dto.setRemainingDays(9999);
        }

        return dto;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        int len = phone.length();
        return phone.substring(0, 3) + "***" + phone.substring(len - 4);
    }

    private String getCrownDisplayName(CrownType type) {
        if (type == null) return "Răng Sứ Cao Cấp";
        switch (type) {
            case ZIRCONIA: return "Răng Sứ Toàn Sứ Zirconia Nhật Bản (Katana)";
            case CERCON_HT: return "Răng Sứ Cercon HT Đức (Dentsply Sirona)";
            case LAVA_PLUS: return "Răng Sứ Cao Cấp Lava Plus 3M (Hoa Kỳ)";
            case EMAX: return "Mặt Dán Sứ Veneer Emax Press (Thụy Sĩ)";
            default: return type.name();
        }
    }
}
