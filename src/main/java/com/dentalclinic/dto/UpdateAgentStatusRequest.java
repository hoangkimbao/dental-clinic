package com.dentalclinic.dto;

import com.dentalclinic.model.AgentStatus;

public class UpdateAgentStatusRequest {
    private Double creditLimit;
    private Boolean active;
    private AgentStatus status;

    public Double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public AgentStatus getStatus() { return status; }
    public void setStatus(AgentStatus status) { this.status = status; }
}
