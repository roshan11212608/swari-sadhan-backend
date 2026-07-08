package swari.sewa.module.enquiry.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.EnquiryStatus;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.shop.entity.Shop;

@Entity
@Table(name = "enquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    
    @Column(name = "customer_phone")
    private String customerPhone;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "preferred_contact_method")
    private String preferredContactMethod;
    
    @Column(name = "budget_range")
    private String budgetRange;
    
    @Column(name = "expected_purchase_time")
    private String expectedPurchaseTime;
    
    @Column(name = "financing_required")
    @Builder.Default
    private Boolean financingRequired = false;
    
    @Column(name = "test_drive_requested")
    @Builder.Default
    private Boolean testDriveRequested = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnquiryStatus status = EnquiryStatus.PENDING;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Manual getters for basic fields
    public Long getId() {
        return id;
    }
    
    public User getCustomer() {
        return customer;
    }
    
    public Vehicle getVehicle() {
        return vehicle;
    }
    
    public EnquiryStatus getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // Additional getters and setters for basic fields
    public Shop getShop() {
        return shop;
    }
    
    public String getResponse() {
        return response;
    }
    
    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public void setStatus(EnquiryStatus status) {
        this.status = status;
    }
    
    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
