package com.dentalclinic.service;

import com.dentalclinic.dto.UpdateStockRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.DentalMaterial;
import com.dentalclinic.model.MaterialCategory;
import com.dentalclinic.repository.DentalMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DentalMaterialService {

    private final DentalMaterialRepository materialRepository;

    public DentalMaterialService(DentalMaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getAllMaterials(MaterialCategory category) {
        if (category != null) {
            return materialRepository.findByCategory(category);
        }
        return materialRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DentalMaterial getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư nha khoa với ID: " + id));
    }

    public DentalMaterial updateStock(Long id, UpdateStockRequest req) {
        DentalMaterial material = getMaterialById(id);
        int change = (req.getStockChange() != null) ? req.getStockChange() : 0;
        int currentStock = (material.getStockQuantity() != null) ? material.getStockQuantity() : 0;
        int newStock = currentStock + change;

        if (newStock < 0) {
            throw new BadRequestException("Số lượng tồn kho không được âm! Hiện có: " + currentStock + ", thay đổi: " + change);
        }

        material.setStockQuantity(newStock);
        return materialRepository.save(material);
    }

    public DentalMaterial saveMaterial(DentalMaterial material) {
        return materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getLowStockAlerts() {
        return materialRepository.findLowStockMaterials();
    }
}
