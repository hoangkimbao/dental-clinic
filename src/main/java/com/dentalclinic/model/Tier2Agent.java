package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tier2_agents", indexes = {
    @Index(name = "idx_t2agent_code", columnList = "agent_code", unique = true),
    @Index(name = "idx_t2agent_city", columnList = "city"),
    @Index(name = "idx_t2agent_type", columnList = "agent_type")
})
public class Tier2Agent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_code", nullable = false, unique = true)
    private String agentCode;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType = AgentType.SATELLITE_CLINIC;

    @Column(name = "representative_name")
    private String representativeName;

    private String phone;
    private String email;
    private String address;
    private String district;
    private String city;
    private String province;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "credit_limit")
    private Double creditLimit = 0.0;

    @Column(name = "current_balance")
    private Double currentBalance = 0.0;

    @Column(name = "outstanding_balance")
    private Double outstandingBalance = 0.0;

    @Column(name = "commission_rate")
    private Double commissionRate = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentStatus status = AgentStatus.ACTIVE;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by_id")
    private User managedBy;

    public Tier2Agent() {}

    public Tier2Agent(String agentCode, String agentName, AgentType agentType,
                      String representativeName, String phone, String email,
                      String address, String district, String city, String province,
                      LocalDate contractDate, Double creditLimit, Double commissionRate) {
        this.agentCode = agentCode;
        this.agentName = agentName;
        this.agentType = agentType;
        this.representativeName = representativeName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.district = district;
        this.city = city;
        this.province = province;
        this.contractDate = contractDate;
        this.creditLimit = creditLimit != null ? creditLimit : 0.0;
        this.commissionRate = commissionRate != null ? commissionRate : 0.0;
        this.currentBalance = 0.0;
        this.outstandingBalance = 0.0;
        this.status = AgentStatus.ACTIVE;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }

    // Alias for getCode / setCode
    public String getCode() { return agentCode; }
    public void setCode(String code) { this.agentCode = code; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public AgentType getAgentType() { return agentType; }
    public void setAgentType(AgentType agentType) { this.agentType = agentType; }

    public String getRepresentativeName() { return representativeName; }
    public void setRepresentativeName(String representativeName) { this.representativeName = representativeName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }

    public Double getCreditLimit() { return creditLimit != null ? creditLimit : 0.0; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit; }

    public Double getCurrentBalance() { return currentBalance != null ? currentBalance : 0.0; }
    public void setCurrentBalance(Double currentBalance) {
        this.currentBalance = currentBalance;
        this.outstandingBalance = currentBalance;
    }

    public Double getOutstandingBalance() { return outstandingBalance != null ? outstandingBalance : getCurrentBalance(); }
    public void setOutstandingBalance(Double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
        this.currentBalance = outstandingBalance;
    }

    public Double getCommissionRate() { return commissionRate != null ? commissionRate : 0.0; }
    public void setCommissionRate(Double commissionRate) { this.commissionRate = commissionRate; }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) {
        this.status = status;
        this.active = (status == AgentStatus.ACTIVE);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? AgentStatus.ACTIVE : AgentStatus.SUSPENDED;
    }

    public User getManagedBy() { return managedBy; }
    public void setManagedBy(User managedBy) { this.managedBy = managedBy; }
}
