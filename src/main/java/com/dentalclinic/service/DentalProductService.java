package com.dentalclinic.service;

import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.DentalProduct;
import com.dentalclinic.model.DentalProductCategory;
import com.dentalclinic.repository.DentalProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DentalProductService {

    private final DentalProductRepository productRepository;

    public DentalProductService(DentalProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<DentalProduct> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DentalProduct getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + id));
    }

    @Transactional(readOnly = true)
    public DentalProduct getProductByCode(String code) {
        return productRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với mã: " + code));
    }

    @Transactional(readOnly = true)
    public List<DentalProduct> getProductsByCategory(DentalProductCategory category) {
        return productRepository.findByCategory(category);
    }

    public DentalProduct saveProduct(DentalProduct product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id);
        }
        productRepository.deleteById(id);
    }
}
