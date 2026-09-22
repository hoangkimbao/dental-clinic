package com.dentalclinic.model;

import com.dentalclinic.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.QR_VNPAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @Column(unique = true, nullable = false)
    private String transactionCode;

    private LocalDateTime paymentTime;

    // Optimistic Locking chống thanh toán lặp
    @Version
    private Long version;

    public Payment() {
        this.paymentTime = LocalDateTime.now();
        this.status = PaymentStatus.SUCCESS;
    }

    public Payment(Appointment appointment, Double amount, PaymentMethod paymentMethod, String transactionCode) {
        this.appointment = appointment;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionCode = transactionCode;
        this.paymentTime = LocalDateTime.now();
        this.status = PaymentStatus.SUCCESS;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
