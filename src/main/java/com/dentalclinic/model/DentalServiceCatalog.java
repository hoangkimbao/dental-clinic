package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "dental_service_catalog", indexes = {
    @Index(name = "idx_service_code", columnList = "code"),
    @Index(name = "idx_service_category", columnList = "category")
})
public class DentalServiceCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DentalServiceCategory category;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Double price;

    private Integer durationMinutes;

    private boolean isFeatured = false;

    public DentalServiceCatalog() {}

    public DentalServiceCatalog(String code, String name, DentalServiceCategory category,
                                String description, Double price, Integer durationMinutes, boolean isFeatured) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
        this.isFeatured = isFeatured;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DentalServiceCategory getCategory() { return category; }
    public void setCategory(DentalServiceCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }
}
