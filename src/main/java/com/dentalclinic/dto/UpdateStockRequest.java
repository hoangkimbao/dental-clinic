package com.dentalclinic.dto;

public class UpdateStockRequest {
    private Integer stockChange;
    private String reason;

    public Integer getStockChange() { return stockChange; }
    public void setStockChange(Integer stockChange) { this.stockChange = stockChange; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
