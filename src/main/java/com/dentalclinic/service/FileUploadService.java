package com.dentalclinic.service;

import com.dentalclinic.model.DentalImageAttachment;
import com.dentalclinic.model.DentalImageType;
import com.dentalclinic.repository.DentalImageRepository;
import com.dentalclinic.security.upload.FileUploadValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${app.upload.dir:./uploads/dental-images/}")
    private String uploadDir;

    private final DentalImageRepository imageRepository;
    private final FileUploadValidator fileUploadValidator;

    public FileUploadService(DentalImageRepository imageRepository, FileUploadValidator fileUploadValidator) {
        this.imageRepository = imageRepository;
        this.fileUploadValidator = fileUploadValidator;
    }

    public DentalImageAttachment uploadDentalImage(MultipartFile file, Long medicalRecordId, Long patientId, String patientName, DentalImageType type, String notes) throws IOException {
        fileUploadValidator.validate(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;
        Path targetLocation = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/uploads/dental-images/" + storedFileName;

        DentalImageAttachment attachment = DentalImageAttachment.builder()
                .medicalRecordId(medicalRecordId)
                .patientId(patientId)
                .patientName(patientName)
                .imageType(type != null ? type : DentalImageType.PANORAMA)
                .fileName(storedFileName)
                .originalName(originalFilename)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .notes(notes)
                .build();

        return imageRepository.save(attachment);
    }

    public List<DentalImageAttachment> getImagesByMedicalRecord(Long medicalRecordId) {
        return imageRepository.findByMedicalRecordIdOrderByUploadedAtDesc(medicalRecordId);
    }

    public List<DentalImageAttachment> getImagesByPatient(Long patientId) {
        return imageRepository.findByPatientIdOrderByUploadedAtDesc(patientId);
    }
}
