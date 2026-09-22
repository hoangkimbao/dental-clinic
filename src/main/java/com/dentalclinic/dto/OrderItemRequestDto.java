package com.dentalclinic.dto;

public class OrderItemRequestDto {
    private Long productId;
    private Long packagingOptionId;
    private Integer quantity;

    public OrderItemRequestDto() {}

    public OrderItemRequestDto(Long productId, Long packagingOptionId, Integer quantity) {
        this.productId = productId;
        this.packagingOptionId = packagingOptionId;
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getPackagingOptionId() { return packagingOptionId; }
    public void setPackagingOptionId(Long packagingOptionId) { this.packagingOptionId = packagingOptionId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
