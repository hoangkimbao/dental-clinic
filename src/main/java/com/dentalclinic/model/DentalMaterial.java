package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dental_materials", indexes = {
    @Index(name = "idx_mat_code", columnList = "material_code", unique = true),
    @Index(name = "idx_mat_category", columnList = "category")
})
public class DentalMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_code", nullable = false, unique = true)
    private String materialCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    private String manufacturer;

    @Column(name = "supplier_name")
    private String supplierName;

    private String unit;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "min_stock_alert", nullable = false)
    private Integer minStockAlert = 10;

    @Column(name = "min_safety_stock")
    private Integer minSafetyStock = 10;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice = 0.0;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    private String specification;

    private boolean active = true;

    public DentalMaterial() {}

    public DentalMaterial(String materialCode, String name, MaterialCategory category,
                          String manufacturer, String unit, Integer stockQuantity,
                          Integer minStockAlert, Double unitPrice, String lotNumber,
                          LocalDate expiryDate, String specification) {
        this.materialCode = materialCode;
        this.name = name;
        this.category = category;
        this.manufacturer = manufacturer;
        this.supplierName = manufacturer;
        this.unit = unit;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.minStockAlert = minStockAlert != null ? minStockAlert : 10;
        this.minSafetyStock = this.minStockAlert;
        this.unitPrice = unitPrice != null ? unitPrice : 0.0;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.specification = specification;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MaterialCategory getCategory() { return category; }
    public void setCategory(MaterialCategory category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        if (this.supplierName == null) this.supplierName = manufacturer;
    }

    public String getSupplierName() { return supplierName != null ? supplierName : manufacturer; }
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
        if (this.manufacturer == null) this.manufacturer = supplierName;
    }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getStockQuantity() { return stockQuantity != null ? stockQuantity : 0; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getMinStockAlert() { return minStockAlert != null ? minStockAlert : 10; }
    public void setMinStockAlert(Integer minStockAlert) {
        this.minStockAlert = minStockAlert;
        this.minSafetyStock = minStockAlert;
    }

    public Integer getMinSafetyStock() { return minSafetyStock != null ? minSafetyStock : getMinStockAlert(); }
    public void setMinSafetyStock(Integer minSafetyStock) {
        this.minSafetyStock = minSafetyStock;
        this.minStockAlert = minSafetyStock;
    }

    public Double getUnitPrice() { return unitPrice != null ? unitPrice : 0.0; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
