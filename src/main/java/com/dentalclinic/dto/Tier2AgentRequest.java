package com.dentalclinic.dto;

import com.dentalclinic.model.AgentStatus;
import com.dentalclinic.model.AgentType;
import java.time.LocalDate;

public class Tier2AgentRequest {
    private String agentCode;
    private String code;
    private String agentName;
    private AgentType agentType = AgentType.SATELLITE_CLINIC;
    private String representativeName;
    private String phone;
    private String email;
    private String address;
    private String district;
    private String city;
    private String province;
    private LocalDate contractDate;
    private Double creditLimit;
    private Double commissionRate;
    private AgentStatus status;
    private Boolean active;

    public String getAgentCode() {
        return agentCode != null ? agentCode : code;
    }
    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
        if (this.code == null) this.code = agentCode;
    }

    public String getCode() {
        return code != null ? code : agentCode;
    }
    public void setCode(String code) {
        this.code = code;
        if (this.agentCode == null) this.agentCode = code;
    }

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

    public Double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit; }

    public Double getCommissionRate() { return commissionRate; }
    public void setCommissionRate(Double commissionRate) { this.commissionRate = commissionRate; }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
