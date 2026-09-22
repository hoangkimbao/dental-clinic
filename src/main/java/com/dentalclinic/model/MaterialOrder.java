package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "material_orders", indexes = {
    @Index(name = "idx_matorder_code", columnList = "order_code", unique = true),
    @Index(name = "idx_matorder_status", columnList = "status")
})
public class MaterialOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id", nullable = false)
    private Tier2Agent agent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialOrderStatus status = MaterialOrderStatus.PENDING_APPROVAL;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", length = 1000)
    private String approvalNotes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MaterialOrderItem> items = new ArrayList<>();

    public MaterialOrder() {
        this.orderDate = LocalDateTime.now();
        this.status = MaterialOrderStatus.PENDING_APPROVAL;
    }

    public MaterialOrder(String orderCode, Tier2Agent agent, User createdBy, String notes) {
        this.orderCode = orderCode;
        this.agent = agent;
        this.createdBy = createdBy;
        this.notes = notes;
        this.orderDate = LocalDateTime.now();
        this.status = MaterialOrderStatus.PENDING_APPROVAL;
        this.totalAmount = 0.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public Tier2Agent getAgent() { return agent; }
    public void setAgent(Tier2Agent agent) { this.agent = agent; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public Double getTotalAmount() { return totalAmount != null ? totalAmount : 0.0; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public MaterialOrderStatus getStatus() { return status; }
    public void setStatus(MaterialOrderStatus status) { this.status = status; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalNotes() { return approvalNotes != null ? approvalNotes : notes; }
    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
        if (this.notes == null) this.notes = approvalNotes;
    }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getNotes() { return notes != null ? notes : approvalNotes; }
    public void setNotes(String notes) {
        this.notes = notes;
        if (this.approvalNotes == null) this.approvalNotes = notes;
    }

    public List<MaterialOrderItem> getItems() { return items; }
    public void setItems(List<MaterialOrderItem> items) {
        this.items = items;
        recalculateTotal();
    }

    public void addItem(MaterialOrderItem item) {
        this.items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void recalculateTotal() {
        if (this.items == null || this.items.isEmpty()) {
            this.totalAmount = 0.0;
            return;
        }
        double sum = 0.0;
        for (MaterialOrderItem item : this.items) {
            sum += (item.getSubtotal() != null ? item.getSubtotal() : 0.0);
        }
        this.totalAmount = sum;
    }
}
