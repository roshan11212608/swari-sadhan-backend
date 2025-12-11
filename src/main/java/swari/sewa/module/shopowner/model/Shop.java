package swari.sewa.module.shopowner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.ShopStatus;
import swari.sewa.module.publicuser.model.User;
import swari.sewa.module.shopowner.model.Vehicle;

import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    // Convenience getter for name
    public String getName() {
        return name;
    }
    
    // Convenience setter for name
    public void setName(String name) {
        this.name = name;
    }
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, unique = true)
    private String licenseNumber;
    
    @Column(name = "address_line_1")
    private String addressLine1;
    
    @Column(name = "address_line_2")
    private String addressLine2;
    
    // Convenience getter for address (combines addressLine1 and addressLine2)
    public String getAddress() {
        if (addressLine1 != null && addressLine2 != null) {
            return addressLine1 + ", " + addressLine2;
        } else if (addressLine1 != null) {
            return addressLine1;
        } else if (addressLine2 != null) {
            return addressLine2;
        }
        return "";
    }
    
    // Convenience setter for address (splits into addressLine1)
    public void setAddress(String address) {
        this.addressLine1 = address;
    }
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private String state;
    
    @Column(nullable = false)
    private String country;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    // Convenience getter for phone (alias for phoneNumber)
    public String getPhone() {
        return phoneNumber;
    }
    
    // Convenience setter for phone (alias for phoneNumber)
    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }
    
    @Column(name = "email_address")
    private String emailAddress;
    
    // Convenience getter for email (alias for emailAddress)
    public String getEmail() {
        return emailAddress;
    }
    
    // Convenience setter for email (alias for emailAddress)
    public void setEmail(String email) {
        this.emailAddress = email;
    }
    
    @Column(name = "website_url")
    private String websiteUrl;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Column(name = "opening_hours")
    private String openingHours;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING_APPROVAL;
    
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;
    
    @Column(name = "subscription_plan")
    private String subscriptionPlan;
    
    @Column(name = "subscription_expiry")
    private LocalDateTime subscriptionExpiry;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_owner_id", nullable = false)
    private ShopOwner shopOwner;
    
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<Vehicle> vehicles = new java.util.HashSet<>();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Manual setter for user
    public void setUser(User user) {
        this.user = user;
    }
    
    // Manual getters and setters for basic fields
    public void setStatus(ShopStatus status) {
        this.status = status;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public Long getId() {
        return id;
    }
    
    public ShopStatus getStatus() {
        return status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
