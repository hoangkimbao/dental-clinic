package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "clinic_branches", indexes = {
    @Index(name = "idx_branch_code", columnList = "code"),
    @Index(name = "idx_branch_city", columnList = "city")
})
public class ClinicBranch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String district;

    private String city;

    private String phone;

    private String hotline;

    private Double latitude;

    private Double longitude;

    private String openingHours;

    @Column(length = 1000)
    private String servicesOffered;

    public ClinicBranch() {}

    public ClinicBranch(String code, String name, String address, String district, String city,
                        String phone, String hotline, Double latitude, Double longitude,
                        String openingHours, String servicesOffered) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.district = district;
        this.city = city;
        this.phone = phone;
        this.hotline = hotline;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openingHours = openingHours;
        this.servicesOffered = servicesOffered;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getServicesOffered() { return servicesOffered; }
    public void setServicesOffered(String servicesOffered) { this.servicesOffered = servicesOffered; }
}
