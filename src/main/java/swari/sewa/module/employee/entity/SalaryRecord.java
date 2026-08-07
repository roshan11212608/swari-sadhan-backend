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
@Table(name = "salary_records")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecord {
    
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
    
    @Column(name = "employee_id_number")
    private String employeeIdNumber;
    
    @Column(name = "month", nullable = false)
    private Integer month;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "month_name")
    private String monthName;
    
    @Column(name = "shop_name")
    private String shopName;
    
    @Column(name = "shop_location")
    private String shopLocation;
    
    @Column(name = "available_days")
    private Integer availableDays;
    
    @Column(name = "paid_days")
    private Integer paidDays;
    
    @Column(name = "loss_of_pay_days")
    private Integer lossOfPayDays;
    
    @Builder.Default
    @Column(name = "loss_of_pay_amount")
    private java.math.BigDecimal lossOfPayAmount = java.math.BigDecimal.ZERO;
    
    @Column(name = "basic_salary", nullable = false)
    private java.math.BigDecimal basicSalary;
    
    @Column(name = "house_allowance")
    private java.math.BigDecimal houseAllowance = java.math.BigDecimal.ZERO;
    
    @Column(name = "travel_allowance")
    private java.math.BigDecimal travelAllowance = java.math.BigDecimal.ZERO;
    
    @Column(name = "medical_allowance")
    private java.math.BigDecimal medicalAllowance = java.math.BigDecimal.ZERO;
    
    @Column(name = "food_allowance")
    private java.math.BigDecimal foodAllowance = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "bonus")
    private java.math.BigDecimal bonus = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "commission")
    private java.math.BigDecimal commission = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "overtime")
    private java.math.BigDecimal overtime = java.math.BigDecimal.ZERO;
    
    @Column(name = "total_earnings", nullable = false)
    private java.math.BigDecimal totalEarnings;
    
    @Builder.Default
    @Column(name = "pf")
    private java.math.BigDecimal pf = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "esi")
    private java.math.BigDecimal esi = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "professional_tax")
    private java.math.BigDecimal professionalTax = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "income_tax")
    private java.math.BigDecimal incomeTax = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "other_deductions")
    private java.math.BigDecimal otherDeductions = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "advance_deduction")
    private java.math.BigDecimal advanceDeduction = java.math.BigDecimal.ZERO;
    
    @Column(name = "total_deductions", nullable = false)
    private java.math.BigDecimal totalDeductions;
    
    @Column(name = "net_salary", nullable = false)
    private java.math.BigDecimal netSalary;
    
    @Builder.Default
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "Pending";
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Builder.Default
    @Column(name = "amount_paid")
    private java.math.BigDecimal amountPaid = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "balance_due")
    private java.math.BigDecimal balanceDue = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "previous_balance")
    private java.math.BigDecimal previousBalance = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "total_payable")
    private java.math.BigDecimal totalPayable = java.math.BigDecimal.ZERO;
    
    @Builder.Default
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();
    
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
