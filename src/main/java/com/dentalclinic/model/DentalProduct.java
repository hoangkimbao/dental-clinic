package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dental_products", indexes = {
    @Index(name = "idx_product_code", columnList = "code"),
    @Index(name = "idx_product_category", columnList = "category")
})
public class DentalProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DentalProductCategory category;

    private String brand;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Double basePrice;

    private Integer stockQuantity = 0;

    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<ProductPackagingOption> packagingOptions = new ArrayList<>();

    public DentalProduct() {}

    public DentalProduct(String code, String name, DentalProductCategory category, String brand,
                         String description, Double basePrice, Integer stockQuantity, String imageUrl) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.description = description;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
    }

    public void addPackagingOption(ProductPackagingOption option) {
        option.setProduct(this);
        this.packagingOptions.add(option);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DentalProductCategory getCategory() { return category; }
    public void setCategory(DentalProductCategory category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<ProductPackagingOption> getPackagingOptions() { return packagingOptions; }
    public void setPackagingOptions(List<ProductPackagingOption> packagingOptions) { this.packagingOptions = packagingOptions; }
}
