package swari.sewa.module.employee.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.module.shop.entity.Shop;

@Entity
@Table(name = "employees")
@Where(clause = "deleted_at IS NULL")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_number", nullable = false, unique = true)
    private String employeeNumber;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(name = "gender", nullable = false)
    private String gender;
    
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    
    @Column(name = "father_name")
    private String fatherName;
    
    @Column(name = "marital_status")
    private String maritalStatus;
    
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;
    
    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;
    
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;
    
    @Column(name = "department")
    private String department;
    
    @Column(name = "designation", nullable = false)
    private String designation;
    
    @Column(name = "employment_type", nullable = false)
    private String employmentType;
    
    @Column(name = "basic_salary", nullable = false)
    private java.math.BigDecimal basicSalary;
    
    @Column(name = "status", nullable = false)
    private String status = "Active";
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(name = "ifsc_code")
    private String ifscCode;
    
    @Column(name = "emergency_contact")
    private String emergencyContact;
    
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;
    
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<Attendance> attendanceRecords = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<SalaryRecord> salaryRecords = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<LeaveRequest> leaveRequests = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<AdvancePayment> advancePayments = new java.util.HashSet<>();
    
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
