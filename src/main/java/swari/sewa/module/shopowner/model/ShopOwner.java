package swari.sewa.module.shopowner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shop_owners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopOwner {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String phone;
    
    @Column(name = "company_name")
    private String companyName;
    
    @Column(name = "license_number")
    private String licenseNumber;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "description")
    private String description;
    
    // Convenience getter for website
    public String getWebsite() {
        return website;
    }
    
    // Convenience getter for description
    public String getDescription() {
        return description;
    }
    
    // Convenience getter for kycVerified
    public Boolean getKycVerified() {
        return kycVerified != null ? kycVerified : Boolean.FALSE;
    }
    
    // Convenience getter for subscriptionActive
    public Boolean getSubscriptionActive() {
        return subscriptionActive != null ? subscriptionActive : Boolean.FALSE;
    }
    
    // Convenience getter for subscriptionPlan
    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }
    
    // Convenience getter for subscriptionExpiresAt
    public LocalDateTime getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }
    
    // Manual getters for basic fields
    public Long getId() {
        return id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getCity() {
        return city;
    }
    
    public String getState() {
        return state;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // Manual setters for basic fields
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    public void setKycVerified(boolean kycVerified) {
        this.kycVerified = kycVerified;
    }
    
    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
    
    public void setSubscriptionActive(boolean subscriptionActive) {
        this.subscriptionActive = subscriptionActive;
    }
    
    public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.SHOP_OWNER;
    
    @Column(nullable = false)
    private Boolean active = false;
    
    // Convenience getter for active (alias for active)
    public boolean isActive() {
        return active != null ? active : false;
    }
    
    // Convenience setter for active (alias for active)
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "kyc_verified")
    private Boolean kycVerified = false;
    
    @Column(name = "subscription_active")
    private Boolean subscriptionActive = false;
    
    @Column(name = "subscription_plan")
    private String subscriptionPlan;
    
    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "shopOwner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shop> shops;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
