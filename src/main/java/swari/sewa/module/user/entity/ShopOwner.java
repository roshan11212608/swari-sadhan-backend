package swari.sewa.module.user.entity;

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

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "citizenship_no")
    private String citizenshipNo;

    @Column(name = "citizenship_pic_front")
    private String citizenshipPicFront;

    @Column(name = "citizenship_pic_back")
    private String citizenshipPicBack;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "shop_type")
    private String shopType;

    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "municipality")
    private String municipality;

    @Column(name = "ward")
    private String ward;

    @Column(name = "tole")
    private String tole;

    @Column(name = "shop_phone")
    private String shopPhone;

    @Column(name = "shop_email")
    private String shopEmail;

    @Column(name = "shop_logo")
    private String shopLogo;

    @Column(name = "pan")
    private String pan;

    @Column(name = "reg_cert")
    private String regCert;

    @Column(name = "vat")
    private String vat;

    @Column(name = "opening_time")
    private String openingTime;

    @Column(name = "closing_time")
    private String closingTime;

    @Column(name = "off_days")
    private String offDays;

    @Column(name = "subscription_plan")
    private String subscriptionPlan;

    @Column(name = "subscription_start_date")
    private String subscriptionStartDate;

    @Column(name = "subscription_expiry_date")
    private String subscriptionExpiryDate;

    @Column(name = "vehicle_limit")
    private Integer vehicleLimit;

    @Column(name = "staff_limit")
    private Integer staffLimit;

    @Column(name = "citizenship_upload")
    private String citizenshipUpload;

    @Column(name = "shop_reg_upload")
    private String shopRegUpload;

    @Column(name = "whatsapp_no")
    private String whatsappNo;

    @Column(name = "facebook_page")
    private String facebookPage;

    @Column(name = "google_map_link")
    private String googleMapLink;

    @Column(name = "notes")
    private String notes;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.SHOP_OWNER;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "kyc_verified")
    private Boolean kycVerified = false;

    @Column(name = "subscription_active")
    private Boolean subscriptionActive = false;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return active != null && active;
    }

    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
}
