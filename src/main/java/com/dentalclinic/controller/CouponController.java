package com.dentalclinic.controller;

import com.dentalclinic.common.ApiResponse;
import com.dentalclinic.dto.CouponResultDto;
import com.dentalclinic.dto.CreateCouponRequest;
import com.dentalclinic.dto.ValidateCouponRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.Coupon;
import com.dentalclinic.repository.CouponRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*")
@Tag(name = "9. Coupons & Promotions", description = "Quản lý mã ưu đãi & Voucher khuyến mãi")
public class CouponController {

    private final CouponRepository couponRepository;

    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @GetMapping("/active")
    @Operation(summary = "Lấy danh sách mã ưu đãi Hot đang áp dụng (Công khai)")
    public ResponseEntity<ApiResponse<List<Coupon>>> getActiveCoupons() {
        return ResponseEntity.ok(ApiResponse.success(couponRepository.findByActiveTrueOrderByCreatedAtDesc()));
    }

    @PostMapping("/validate")
    @Operation(summary = "Kiểm tra và áp dụng mã voucher (Công khai)")
    public ResponseEntity<ApiResponse<CouponResultDto>> validateCoupon(@Valid @RequestBody ValidateCouponRequest request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(request.getCode().trim())
                .orElseThrow(() -> new BadRequestException("Mã ưu đãi '" + request.getCode() + "' không tồn tại hoặc đã hết hạn!"));

        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDate.now())) {
            throw new BadRequestException("Rất tiếc! Mã ưu đãi này đã hết hạn sử dụng.");
        }

        String desc = coupon.getDiscountType().equals("FIXED_AMOUNT") 
                ? "Giảm ngay " + String.format("%,.0f", coupon.getDiscountValue()) + " VNĐ"
                : "Giảm " + coupon.getDiscountValue().intValue() + "% tổng hóa đơn";

        CouponResultDto result = new CouponResultDto(
                coupon.getCode(),
                coupon.getTitle(),
                desc,
                coupon.getDiscountValue()
        );

        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã ưu đãi thành công!", result));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Tạo mã ưu đãi mới (Chỉ Chủ phòng khám & Lễ tân)")
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        String cleanCode = request.getCode().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCaseAndActiveTrue(cleanCode).isPresent()) {
            throw new BadRequestException("Mã ưu đãi '" + cleanCode + "' đã tồn tại trên hệ thống!");
        }

        LocalDate validUntil = (request.getValidDays() != null && request.getValidDays() > 0)
                ? LocalDate.now().plusDays(request.getValidDays())
                : LocalDate.now().plusMonths(1);

        Coupon coupon = new Coupon(
                cleanCode,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : "",
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getApplicableService(),
                validUntil
        );

        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo thành công mã ưu đãi: " + saved.getCode(), saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'RECEPTIONIST')")
    @Operation(summary = "Hủy / Xóa mã ưu đãi (Chỉ Chủ phòng khám & Lễ tân)")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã ưu đãi #" + id));
        coupon.setActive(false);
        couponRepository.save(coupon);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mã ưu đãi thành công!", null));
    }
}
