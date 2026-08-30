package swari.sewa.module.wishlist.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.module.user.entity.User;
import swari.sewa.module.vehicle.entity.Vehicle;

@Entity
@Table(name = "wishlists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    // Convenience getter for vehicle
    public Vehicle getVehicle() {
        return vehicle;
    }
    
    // Convenience setter for vehicle
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
    
    // Manual getters for basic fields
    public User getCustomer() {
        return customer;
    }
    
    public Long getVehicleId() {
        return vehicle != null ? vehicle.getId() : null;
    }
    
    public String getVehicleTitle() {
        return vehicle != null ? vehicle.getTitle() : null;
    }
}
