package com.dentalclinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dental_image_attachments")
public class DentalImageAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long medicalRecordId;
    
    private Long patientId;
    
    private String patientName;

    @Enumerated(EnumType.STRING)
    private DentalImageType imageType;

    private String fileName;
    
    private String originalName;
    
    private String fileUrl;
    
    private Long fileSize;
    
    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime uploadedAt;

    public DentalImageAttachment() {}

    public DentalImageAttachment(Long id, Long medicalRecordId, Long patientId, String patientName, DentalImageType imageType, String fileName, String originalName, String fileUrl, Long fileSize, String contentType, String notes, LocalDateTime uploadedAt) {
        this.id = id;
        this.medicalRecordId = medicalRecordId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.imageType = imageType;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.notes = notes;
        this.uploadedAt = uploadedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }

    // Builder pattern helper
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long medicalRecordId;
        private Long patientId;
        private String patientName;
        private DentalImageType imageType;
        private String fileName;
        private String originalName;
        private String fileUrl;
        private Long fileSize;
        private String contentType;
        private String notes;
        private LocalDateTime uploadedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder medicalRecordId(Long medicalRecordId) { this.medicalRecordId = medicalRecordId; return this; }
        public Builder patientId(Long patientId) { this.patientId = patientId; return this; }
        public Builder patientName(String patientName) { this.patientName = patientName; return this; }
        public Builder imageType(DentalImageType imageType) { this.imageType = imageType; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder originalName(String originalName) { this.originalName = originalName; return this; }
        public Builder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder notes(String notes) { this.notes = notes; return this; }
        public Builder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }

        public DentalImageAttachment build() {
            return new DentalImageAttachment(id, medicalRecordId, patientId, patientName, imageType, fileName, originalName, fileUrl, fileSize, contentType, notes, uploadedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(Long medicalRecordId) { this.medicalRecordId = medicalRecordId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public DentalImageType getImageType() { return imageType; }
    public void setImageType(DentalImageType imageType) { this.imageType = imageType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
