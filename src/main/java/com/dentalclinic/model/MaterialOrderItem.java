package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "material_order_items")
public class MaterialOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private MaterialOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private DentalMaterial material;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "quantity_requested")
    private Integer quantityRequested = 1;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice = 0.0;

    @Column(nullable = false)
    private Double subtotal = 0.0;

    public MaterialOrderItem() {}

    public MaterialOrderItem(MaterialOrder order, DentalMaterial material, Integer quantity, Double unitPrice) {
        this.order = order;
        this.material = material;
        this.quantity = quantity != null ? quantity : 1;
        this.quantityRequested = this.quantity;
        this.unitPrice = unitPrice != null ? unitPrice : 0.0;
        this.subtotal = this.quantity * this.unitPrice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MaterialOrder getOrder() { return order; }
    public void setOrder(MaterialOrder order) { this.order = order; }

    public DentalMaterial getMaterial() { return material; }
    public void setMaterial(DentalMaterial material) { this.material = material; }

    public Integer getQuantity() { return quantity != null ? quantity : quantityRequested; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.quantityRequested = quantity;
        recalculateSubtotal();
    }

    public Integer getQuantityRequested() { return quantityRequested != null ? quantityRequested : quantity; }
    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        this.quantity = quantityRequested;
        recalculateSubtotal();
    }

    public Double getUnitPrice() { return unitPrice != null ? unitPrice : 0.0; }
    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
        recalculateSubtotal();
    }

    public Double getSubtotal() { return subtotal != null ? subtotal : (getQuantity() * getUnitPrice()); }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    private void recalculateSubtotal() {
        int qty = (this.quantity != null) ? this.quantity : 0;
        double price = (this.unitPrice != null) ? this.unitPrice : 0.0;
        this.subtotal = qty * price;
    }
}
