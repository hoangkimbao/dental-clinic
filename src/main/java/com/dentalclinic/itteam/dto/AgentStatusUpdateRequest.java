package com.dentalclinic.itteam.dto;

import java.util.Set;

public class AgentStatusUpdateRequest {
    private String status;

    private static final Set<String> VALID_STATUSES = Set.of("ONLINE", "OFFLINE", "BUSY", "ACTIVE", "AWAY");

    public AgentStatusUpdateRequest() {
    }

    public AgentStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isValid() {
        return status != null && VALID_STATUSES.contains(status.toUpperCase());
    }
}
