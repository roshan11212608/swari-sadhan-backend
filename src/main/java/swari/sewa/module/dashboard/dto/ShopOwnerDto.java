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
public class ShopOwnerDto {
    // Basic Info
    private Long id;
    private String fullName;
    private String firstName;
    private String lastName;
    private String ownerName;
    private String fatherName;
    private String address;
    private String phone;
    private String email;
    private String password;
    private String confirmPassword;
    private String profilePhoto;
    private String citizenshipNo;
    private String citizenshipPicFront;
    private String citizenshipPicBack;

    // Shop Details
    private String shopName;
    private String companyName;
    private String shopType;
    private String province;
    private String district;
    private String municipality;
    private String ward;
    private String tole;
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
    private String plan;
    private String startDate;
    private String expiryDate;
    private Integer vehicleLimit;
    private Integer staffLimit;
    private String[] permissions;
    private String status;

    // Important Data
    private String citizenshipUpload;
    private String shopRegUpload;
    private String whatsappNo;
    private String facebookPage;
    private String googleMapLink;
    private String notes;

    // Status
    private Boolean active;
    private LocalDateTime createdAt;

}
