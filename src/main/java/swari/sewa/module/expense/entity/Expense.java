package swari.sewa.module.expense.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.ExpensePaymentMethod;
import swari.sewa.common.enums.ExpensePaymentStatus;
import swari.sewa.module.shop.entity.Shop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "expense_number", nullable = false, unique = true)
    private String expenseNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "vendor_paid_to")
    private String vendorPaidTo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private ExpensePaymentMethod paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private ExpensePaymentStatus paymentStatus = ExpensePaymentStatus.PENDING;
    
    @Column(name = "reference_number")
    private String referenceNumber;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "attachment_path")
    private String attachmentPath;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<ExpenseAttachment> attachments = new HashSet<>();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Manual getters for basic fields
    public Long getId() {
        return id;
    }
    
    public String getExpenseNumber() {
        return expenseNumber;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public LocalDate getExpenseDate() {
        return expenseDate;
    }
    
    public ExpensePaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public ExpensePaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
}
