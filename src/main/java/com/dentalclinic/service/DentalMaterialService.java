package com.dentalclinic.service;

import com.dentalclinic.dto.UpdateStockRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.DentalMaterial;
import com.dentalclinic.model.MaterialCategory;
import com.dentalclinic.repository.DentalMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    public DentalMaterial deductStockForTreatment(Long materialId, int quantityUsed) {
        if (quantityUsed <= 0) {
            throw new BadRequestException("Số lượng vật tư xuất tiêu hao phải lớn hơn 0!");
        }
        DentalMaterial material = getMaterialById(materialId);
        int currentStock = material.getStockQuantity() != null ? material.getStockQuantity() : 0;
        if (currentStock < quantityUsed) {
            throw new BadRequestException("Không đủ số lượng tồn kho vật tư '" + material.getName()
                    + "'! Hiện có: " + currentStock + ", cần xuất: " + quantityUsed);
        }
        material.setStockQuantity(currentStock - quantityUsed);
        return materialRepository.save(material);
    }

    public DentalMaterial saveMaterial(DentalMaterial material) {
        if (material.getMaterialCode() == null || material.getMaterialCode().isBlank()) {
            material.setMaterialCode("MAT-" + System.currentTimeMillis());
        }
        if (material.getCategory() == null) {
            material.setCategory(MaterialCategory.CONSUMABLE);
        }
        return materialRepository.save(material);
    }

    public DentalMaterial updateMaterial(Long id, DentalMaterial req) {
        DentalMaterial existing = getMaterialById(id);
        if (req.getName() != null && !req.getName().isBlank()) existing.setName(req.getName().trim());
        if (req.getUnit() != null) existing.setUnit(req.getUnit().trim());
        if (req.getCategory() != null) existing.setCategory(req.getCategory());
        if (req.getStockQuantity() != null) {
            if (req.getStockQuantity() < 0) {
                throw new BadRequestException("Số lượng tồn kho không được âm!");
            }
            existing.setStockQuantity(req.getStockQuantity());
        }
        if (req.getMinStockAlert() != null) existing.setMinStockAlert(req.getMinStockAlert());
        if (req.getUnitPrice() != null) existing.setUnitPrice(req.getUnitPrice());
        if (req.getExpiryDate() != null) existing.setExpiryDate(req.getExpiryDate());
        if (req.getManufacturer() != null) existing.setManufacturer(req.getManufacturer());
        if (req.getLotNumber() != null) existing.setLotNumber(req.getLotNumber());
        if (req.getSpecification() != null) existing.setSpecification(req.getSpecification());
        return materialRepository.save(existing);
    }

    public void deleteMaterial(Long id) {
        DentalMaterial existing = getMaterialById(id);
        existing.setActive(false);
        materialRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getLowStockAlerts() {
        return materialRepository.findLowStockMaterials();
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getLowStockMaterials() {
        return materialRepository.findLowStockMaterials();
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getExpiringMaterials(int daysAhead) {
        LocalDate thresholdDate = LocalDate.now().plusDays(Math.max(1, daysAhead));
        return materialRepository.findExpiringMaterials(thresholdDate);
    }

    @Transactional(readOnly = true)
    public List<DentalMaterial> getExpiringMaterials() {
        return getExpiringMaterials(30);
    }
}
