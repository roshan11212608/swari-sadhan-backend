package swari.sewa.module.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsDto {
    private Long id;
    private String email;
    private String password;
    private String role;
    private String userType; // "SHOP_OWNER" or "PUBLIC_USER"
    private String status; // "ACTIVE", "INACTIVE", "BLOCKED"
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String name;
    private String phone;
    private String shopName; // Only for shop owners
    private Boolean isEmailVerified;
    private String verificationToken;
    private LocalDateTime emailVerifiedAt;

    // Owner Information
    private String firstName;
    private String lastName;
    private String fatherName;
    private String address;
    private String citizenshipNo;
    private String profilePhoto;
    private String citizenshipPicFront;
    private String citizenshipPicBack;

    // Location fields for shop owners
    private String province;
    private String district;
    private String municipality;
    private String ward;
    private String tole;

    // Shop Details
    private String shopType;
    private String companyName;
    private String shopPhone;
    private String shopEmail;
    private String shopLogo;
    private String pan;
    private String regCert;
    private String vat;
    private String openingTime;
    private String closingTime;
    private String offDays;

    // Subscription Details
    private String subscriptionPlan;
    private String subscriptionStartDate;
    private String subscriptionExpiryDate;
    private Integer vehicleLimit;
    private Integer staffLimit;

    // Important Data & Links
    private String citizenshipUpload;
    private String shopRegUpload;
    private String whatsappNo;
    private String facebookPage;
    private String googleMapLink;
    private String notes;

    // Additional fields
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String website;
    private String description;
}
