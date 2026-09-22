package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
public class Coupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String discountType; // FIXED_AMOUNT, PERCENTAGE

    @Column(nullable = false)
    private Double discountValue; // VD: 500000 (VND) hoặc 50 (%)

    private String applicableService; // ALL, NIENG_RANG, IMPLANT, TAY_TRANG...
    private LocalDate validUntil;
    private boolean active = true;

    public Coupon() {}

    public Coupon(String code, String title, String description, String discountType, Double discountValue, String applicableService, LocalDate validUntil) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.applicableService = applicableService;
        this.validUntil = validUntil;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
    public String getApplicableService() { return applicableService; }
    public void setApplicableService(String applicableService) { this.applicableService = applicableService; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
