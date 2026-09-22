package com.dentalclinic.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmsConfigDto {

    private String clinicName;
    private String tagline;
    private String hotline;
    private String emergencyHotline;
    private String email;
    private String workingHours;
    private String mainAddress;
    private List<MenuItemDto> menuItems = new ArrayList<>();
    private FooterConfigDto footerConfig = new FooterConfigDto();
    private LocalDateTime updatedAt;
    private String updatedBy;

    public CmsConfigDto() {
        this.updatedAt = LocalDateTime.now();
    }

    public static CmsConfigDto createDefault() {
        CmsConfigDto config = new CmsConfigDto();
        config.setClinicName("DentalCare Luxury Dental Clinic");
        config.setTagline("Hệ thống Nha khoa Kỹ thuật số Tiêu chuẩn Quốc tế");
        config.setHotline("1900 6868");
        config.setEmergencyHotline("0977 224 504 (24/7)");
        config.setEmail("contact@nhakhoadentalcare.id.vn");
        config.setWorkingHours("Thứ 2 - Chủ Nhật: 08:00 - 20:00");
        config.setMainAddress("386 Chợ Lớn, Phường 11, Quận 5, TP. Hồ Chí Minh");
        config.setUpdatedBy("SYSTEM_INIT");
        config.setUpdatedAt(LocalDateTime.now());

        // Default navigation menu items
        List<MenuItemDto> items = new ArrayList<>();
        items.add(new MenuItemDto(1L, "Trang Chủ", "#home", "fa-house", 1, true, null));
        items.add(new MenuItemDto(2L, "Dịch Vụ Nha Khoa", "#services", "fa-tooth", 2, true, "HOT"));
        items.add(new MenuItemDto(3L, "Đội Ngũ Bác Sĩ", "#dentists", "fa-user-doctor", 3, true, null));
        items.add(new MenuItemDto(4L, "Sản Phẩm & Vật Tư", "#products", "fa-box-open", 4, true, null));
        items.add(new MenuItemDto(5L, "Tra Cứu Bảo Hành", "#warranty", "fa-shield-halved", 5, true, null));
        items.add(new MenuItemDto(6L, "Chẩn Đoán AI", "#ai-diagnostic", "fa-brain", 6, true, "AI 3D"));
        items.add(new MenuItemDto(7L, "Hệ Thống Chi Nhánh", "#branches", "fa-location-dot", 7, true, null));
        items.add(new MenuItemDto(8L, "Diễn Đàn & Hỏi Đáp", "#forum", "fa-comments", 8, true, null));
        config.setMenuItems(items);

        // Default footer
        FooterConfigDto footer = new FooterConfigDto();
        footer.setAboutText("DentalCare Luxury Clinic tiên phong ứng dụng công nghệ nha khoa số 3D, cấy ghép Implant Thụy Sĩ, chỉnh nha không đau và vô trùng chuẩn y tế quốc tế.");
        footer.setCopyrightText("© 2026 DentalCare Luxury Clinic. All rights reserved.");
        footer.setLicenseNumber("Giấy phép hoạt động khám chữa bệnh số: 08688/HCM-GPHĐ cấp bởi Sở Y Tế TP.HCM");

        Map<String, String> socials = new HashMap<>();
        socials.put("facebook", "https://facebook.com/dentalcareluxury");
        socials.put("zalo", "https://zalo.me/0977224504");
        socials.put("youtube", "https://youtube.com/@dentalcareluxury");
        socials.put("tiktok", "https://tiktok.com/@dentalcareluxury");
        footer.setSocialLinks(socials);

        List<QuickLinkDto> quickLinks = new ArrayList<>();
        quickLinks.add(new QuickLinkDto("Chính Sách Bảo Hành Răng Sứ", "/policy/warranty"));
        quickLinks.add(new QuickLinkDto("Bảng Giá Dịch Vụ Niêm Yết", "/pricing"));
        quickLinks.add(new QuickLinkDto("Chính Sách Hoàn Cọc 24H", "/policy/deposit-refund"));
        quickLinks.add(new QuickLinkDto("Hướng Dẫn Chăm Sóc Răng Miệng", "/care-guide"));
        footer.setQuickLinks(quickLinks);

        config.setFooterConfig(footer);
        return config;
    }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }

    public String getEmergencyHotline() { return emergencyHotline; }
    public void setEmergencyHotline(String emergencyHotline) { this.emergencyHotline = emergencyHotline; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }

    public String getMainAddress() { return mainAddress; }
    public void setMainAddress(String mainAddress) { this.mainAddress = mainAddress; }

    public List<MenuItemDto> getMenuItems() { return menuItems; }
    public void setMenuItems(List<MenuItemDto> menuItems) { this.menuItems = menuItems; }

    public FooterConfigDto getFooterConfig() { return footerConfig; }
    public void setFooterConfig(FooterConfigDto footerConfig) { this.footerConfig = footerConfig; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Nested DTOs
    public static class MenuItemDto {
        private Long id;
        private String title;
        private String path;
        private String icon;
        private int orderIndex;
        private boolean active;
        private String badge;

        public MenuItemDto() {}

        public MenuItemDto(Long id, String title, String path, String icon, int orderIndex, boolean active, String badge) {
            this.id = id;
            this.title = title;
            this.path = path;
            this.icon = icon;
            this.orderIndex = orderIndex;
            this.active = active;
            this.badge = badge;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public int getOrderIndex() { return orderIndex; }
        public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public String getBadge() { return badge; }
        public void setBadge(String badge) { this.badge = badge; }
    }

    public static class FooterConfigDto {
        private String aboutText;
        private String copyrightText;
        private String licenseNumber;
        private Map<String, String> socialLinks = new HashMap<>();
        private List<QuickLinkDto> quickLinks = new ArrayList<>();

        public FooterConfigDto() {}

        public String getAboutText() { return aboutText; }
        public void setAboutText(String aboutText) { this.aboutText = aboutText; }

        public String getCopyrightText() { return copyrightText; }
        public void setCopyrightText(String copyrightText) { this.copyrightText = copyrightText; }

        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

        public Map<String, String> getSocialLinks() { return socialLinks; }
        public void setSocialLinks(Map<String, String> socialLinks) { this.socialLinks = socialLinks; }

        public List<QuickLinkDto> getQuickLinks() { return quickLinks; }
        public void setQuickLinks(List<QuickLinkDto> quickLinks) { this.quickLinks = quickLinks; }
    }

    public static class QuickLinkDto {
        private String title;
        private String url;

        public QuickLinkDto() {}

        public QuickLinkDto(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
