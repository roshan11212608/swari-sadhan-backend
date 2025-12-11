package swari.sewa.module.publicuser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.shopowner.model.Shop;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    // Convenience getter for active (alias for isActive)
    public boolean getActive() {
        return isActive != null ? isActive : true;
    }
    
    // Convenience setter for active (alias for isActive)
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    // Manual getters for basic fields
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public Long getId() {
        return id;
    }
    
    public boolean isActive() {
        return isActive != null ? isActive : true;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // Manual setters for basic fields
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setIsEmailVerified(boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }
    
    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Shop> shops = new HashSet<>();
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Enquiry> enquiries = new HashSet<>();
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
