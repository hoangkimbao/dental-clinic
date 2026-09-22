package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "loyalty_accounts", indexes = {
    @Index(name = "idx_loyalty_phone", columnList = "phone")
})
public class LoyaltyAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private Integer pointsBalance = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyTier membershipTier = LoyaltyTier.SILVER;

    @Column(nullable = false)
    private Integer totalPointsEarned = 0;

    public LoyaltyAccount() {}

    public LoyaltyAccount(User patient, String phone, Integer pointsBalance, LoyaltyTier membershipTier, Integer totalPointsEarned) {
        this.patient = patient;
        this.phone = phone;
        this.pointsBalance = pointsBalance;
        this.membershipTier = membershipTier;
        this.totalPointsEarned = totalPointsEarned;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(Integer pointsBalance) { this.pointsBalance = pointsBalance; }

    public LoyaltyTier getMembershipTier() { return membershipTier; }
    public void setMembershipTier(LoyaltyTier membershipTier) { this.membershipTier = membershipTier; }

    public Integer getTotalPointsEarned() { return totalPointsEarned; }
    public void setTotalPointsEarned(Integer totalPointsEarned) { this.totalPointsEarned = totalPointsEarned; }
}
