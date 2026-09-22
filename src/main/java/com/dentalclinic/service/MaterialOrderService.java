package com.dentalclinic.service;

import com.dentalclinic.dto.CreateMaterialOrderRequest;
import com.dentalclinic.dto.MaterialOrderItemRequest;
import com.dentalclinic.dto.UpdateOrderStatusRequest;
import com.dentalclinic.exception.BadRequestException;
import com.dentalclinic.exception.ResourceNotFoundException;
import com.dentalclinic.model.*;
import com.dentalclinic.repository.DentalMaterialRepository;
import com.dentalclinic.repository.MaterialOrderRepository;
import com.dentalclinic.repository.Tier2AgentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MaterialOrderService {

    private final MaterialOrderRepository orderRepository;
    private final Tier2AgentRepository agentRepository;
    private final DentalMaterialRepository materialRepository;

    public MaterialOrderService(MaterialOrderRepository orderRepository,
                                Tier2AgentRepository agentRepository,
                                DentalMaterialRepository materialRepository) {
        this.orderRepository = orderRepository;
        this.agentRepository = agentRepository;
        this.materialRepository = materialRepository;
    }

    public MaterialOrder createOrder(CreateMaterialOrderRequest req, User currentUser) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BadRequestException("Danh sách mặt hàng đặt không được để trống!");
        }
        if (req.getAgentId() == null) {
            throw new BadRequestException("Đại lý đặt hàng không được để trống!");
        }

        Tier2Agent agent = agentRepository.findById(req.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đại lý với ID: " + req.getAgentId()));

        double totalAmount = 0.0;
        for (MaterialOrderItemRequest itemReq : req.getItems()) {
            if (itemReq.getMaterialId() == null) {
                throw new BadRequestException("Mã định danh vật tư không được để trống!");
            }
            int qty = itemReq.getQuantity();
            if (qty <= 0) {
                throw new BadRequestException("Số lượng đặt phải lớn hơn 0!");
            }
            DentalMaterial mat = materialRepository.findById(itemReq.getMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật tư với ID: " + itemReq.getMaterialId()));
            double price = (itemReq.getUnitPrice() != null && itemReq.getUnitPrice() > 0) ? itemReq.getUnitPrice() : mat.getUnitPrice();
            totalAmount += (qty * price);
        }

        // Check credit limit
        if (agent.getCreditLimit() != null && agent.getCreditLimit() > 0) {
            double currentBal = agent.getCurrentBalance() != null ? agent.getCurrentBalance() : 0.0;
            if (currentBal + totalAmount > agent.getCreditLimit()) {
                throw new BadRequestException("Đơn hàng vượt quá hạn mức công nợ khả dụng của đại lý! Hạn mức: "
                        + agent.getCreditLimit() + ", Công nợ hiện tại: " + currentBal + ", Giá trị đơn: " + totalAmount);
            }
        }

        String orderCode = "ORD-MAT-" + LocalDate.now().getYear() + "-" + String.format("%04d", (orderRepository.count() + 1));
        MaterialOrder order = new MaterialOrder(orderCode, agent, currentUser, req.getNotes());
        order.setTotalAmount(totalAmount);

        for (MaterialOrderItemRequest itemReq : req.getItems()) {
            DentalMaterial mat = materialRepository.findById(itemReq.getMaterialId()).orElse(null);
            double price = (itemReq.getUnitPrice() != null && itemReq.getUnitPrice() > 0) ? itemReq.getUnitPrice() : (mat != null ? mat.getUnitPrice() : 0.0);
            MaterialOrderItem orderItem = new MaterialOrderItem(order, mat, itemReq.getQuantity(), price);
            order.addItem(orderItem);
        }

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<MaterialOrder> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional(readOnly = true)
    public MaterialOrder getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng vật tư với ID: " + id));
    }

    public MaterialOrder updateOrderStatus(Long id, UpdateOrderStatusRequest req, User approvedBy) {
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            throw new BadRequestException("Trạng thái cập nhật không được để trống!");
        }

        String statusStr = req.getStatus().trim().toUpperCase();
        if ("REJECTED".equals(statusStr) && (req.getNotes() == null || req.getNotes().trim().isEmpty())) {
            throw new BadRequestException("Lý do từ chối không được để trống!");
        }

        MaterialOrder order = getOrderById(id);

        if ("APPROVED".equals(statusStr) && order.getStatus() != MaterialOrderStatus.APPROVED) {
            // Decrement warehouse stock for each ordered item
            for (MaterialOrderItem item : order.getItems()) {
                DentalMaterial mat = item.getMaterial();
                int currentStock = (mat.getStockQuantity() != null) ? mat.getStockQuantity() : 0;
                int reqQty = item.getQuantity();
                mat.setStockQuantity(Math.max(0, currentStock - reqQty));
                materialRepository.save(mat);
            }

            // Sync agent current balance
            Tier2Agent agent = order.getAgent();
            if (agent != null) {
                double currentBal = (agent.getCurrentBalance() != null) ? agent.getCurrentBalance() : 0.0;
                agent.setCurrentBalance(currentBal + order.getTotalAmount());
                agentRepository.save(agent);
            }

            order.setApprovedBy(approvedBy);
            order.setApprovedAt(LocalDateTime.now());
        }

        try {
            order.setStatus(MaterialOrderStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ: " + req.getStatus());
        }

        order.setApprovalNotes(req.getNotes());
        order.setNotes(req.getNotes());
        if ("REJECTED".equals(statusStr)) {
            order.setRejectionReason(req.getNotes());
        }

        return orderRepository.save(order);
    }
}
