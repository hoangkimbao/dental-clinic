package com.dentalclinic.dto;

public class MaterialOrderItemRequest {
    private Long materialId;
    private Integer quantity;
    private Integer quantityRequested;
    private Double unitPrice;

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public Integer getQuantity() {
        return quantity != null ? quantity : quantityRequested;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.quantityRequested == null) this.quantityRequested = quantity;
    }

    public Integer getQuantityRequested() {
        return quantityRequested != null ? quantityRequested : quantity;
    }
    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        if (this.quantity == null) this.quantity = quantityRequested;
    }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
}
