package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "dental_order_items")
public class DentalOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private DentalOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private DentalProduct product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "packaging_option_id")
    private ProductPackagingOption packagingOption;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double subtotal;

    public DentalOrderItem() {}

    public DentalOrderItem(DentalOrder order, DentalProduct product, ProductPackagingOption packagingOption,
                           Integer quantity, Double unitPrice, Double subtotal) {
        this.order = order;
        this.product = product;
        this.packagingOption = packagingOption;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DentalOrder getOrder() { return order; }
    public void setOrder(DentalOrder order) { this.order = order; }

    public DentalProduct getProduct() { return product; }
    public void setProduct(DentalProduct product) { this.product = product; }

    public ProductPackagingOption getPackagingOption() { return packagingOption; }
    public void setPackagingOption(ProductPackagingOption packagingOption) { this.packagingOption = packagingOption; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
}
