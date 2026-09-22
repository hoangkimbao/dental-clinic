package com.dentalclinic.service;

import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.DentalServiceCatalog;
import com.dentalclinic.model.DentalServiceCategory;
import com.dentalclinic.repository.DentalServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DentalServiceCatalogService {

    private final DentalServiceCatalogRepository serviceRepository;

    public DentalServiceCatalogService(DentalServiceCatalogRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<DentalServiceCatalog> getAllServices() {
        return serviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DentalServiceCatalog> getFeaturedServices() {
        return serviceRepository.findByIsFeaturedTrue();
    }

    @Transactional(readOnly = true)
    public List<DentalServiceCatalog> getServicesByCategory(DentalServiceCategory category) {
        return serviceRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public DentalServiceCatalog getServiceByCode(String code) {
        return serviceRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại với mã: " + code));
    }

    public DentalServiceCatalog saveService(DentalServiceCatalog service) {
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy dịch vụ để xóa với id: " + id);
        }
        serviceRepository.deleteById(id);
    }
}
