package com.dentalclinic.dto;

import java.util.List;

public class CreateMaterialOrderRequest {
    private Long agentId;
    private String notes;
    private List<MaterialOrderItemRequest> items;

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<MaterialOrderItemRequest> getItems() { return items; }
    public void setItems(List<MaterialOrderItemRequest> items) { this.items = items; }
}
