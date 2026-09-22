package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "product_packaging_options")
public class ProductPackagingOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private DentalProduct product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackagingType packagingType;

    @Column(nullable = false)
    private String unitName;

    private Integer itemsPerPackage;

    private Double discountPercent = 0.0;

    @Column(nullable = false)
    private Double price;

    public ProductPackagingOption() {}

    public ProductPackagingOption(DentalProduct product, PackagingType packagingType, String unitName,
                                  Integer itemsPerPackage, Double discountPercent, Double price) {
        this.product = product;
        this.packagingType = packagingType;
        this.unitName = unitName;
        this.itemsPerPackage = itemsPerPackage;
        this.discountPercent = discountPercent;
        this.price = price;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DentalProduct getProduct() { return product; }
    public void setProduct(DentalProduct product) { this.product = product; }

    public PackagingType getPackagingType() { return packagingType; }
    public void setPackagingType(PackagingType packagingType) { this.packagingType = packagingType; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Integer getItemsPerPackage() { return itemsPerPackage; }
    public void setItemsPerPackage(Integer itemsPerPackage) { this.itemsPerPackage = itemsPerPackage; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
