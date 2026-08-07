package swari.sewa.module.employee.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Where;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "advance_payments")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancePayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Long version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(name = "employee_name")
    private String employeeName;
    
    @Column(name = "employee_photo_url")
    private String employeePhotoUrl;
    
    @Column(name = "designation")
    private String designation;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "advance_amount", nullable = false)
    private java.math.BigDecimal advanceAmount;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "recovery_method")
    private String recoveryMethod;
    
    @Builder.Default
    @Column(name = "monthly_deduction")
    private java.math.BigDecimal monthlyDeduction = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "recovered_amount")
    private java.math.BigDecimal recoveredAmount = java.math.BigDecimal.ZERO;
    
    @Column(name = "remaining_balance", nullable = false)
    private java.math.BigDecimal remainingBalance;
    
    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "Pending";
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_date")
    private LocalDate approvedDate;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
