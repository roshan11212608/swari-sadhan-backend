package swari.sewa.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopOwnerProfileDto {
    private Long id;

    private Long shopId;
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phone;
    
    @Size(max = 100, message = "Company name must be less than 100 characters")
    private String companyName;
    
    @Size(max = 50, message = "License number must be less than 50 characters")
    private String licenseNumber;

    private String profilePhoto;
    
    @Size(max = 200, message = "Address must be less than 200 characters")
    private String address;
    
    @Size(max = 50, message = "City must be less than 50 characters")
    private String city;
    
    @Size(max = 50, message = "State must be less than 50 characters")
    private String state;
    
    @Size(max = 20, message = "Postal code must be less than 20 characters")
    private String postalCode;
    
    @Size(max = 50, message = "Country must be less than 50 characters")
    private String country;
    
    @Size(max = 200, message = "Website must be less than 200 characters")
    private String website;
    
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    private Boolean kycVerified;
    private Boolean subscriptionActive;
    private String subscriptionPlan;
    private LocalDateTime subscriptionExpiresAt;
    private LocalDateTime createdAt;
    
    // Manual getters for basic fields
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public String getAddress() {
        return address;
    }

    public String getProfilePhoto() {
        return profilePhoto;
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
    
    public String getWebsite() {
        return website;
    }
    
    public String getDescription() {
        return description;
    }
}
