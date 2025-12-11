package swari.sewa.module.publicuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDto {
    private Long id;
    
    private LocalDateTime createdAt;
    
    private Long customerId;
    
    private Long vehicleId;
    
    private String customerName;
    
    private String vehicleTitle;
    
    private String vehicleMainImageUrl;
    
    private BigDecimal vehiclePrice;
    
    // Convenience setter for vehiclePrice
    public void setVehiclePrice(BigDecimal vehiclePrice) {
        this.vehiclePrice = vehiclePrice;
    }
    
    private String shopName;
    
    // Convenience setter for shopName
    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    // Convenience setter for vehicleMainImageUrl
    public void setVehicleMainImageUrl(String vehicleMainImageUrl) {
        this.vehicleMainImageUrl = vehicleMainImageUrl;
    }
    
    // Manual setters for basic fields
    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    public void setVehicleTitle(String vehicleTitle) {
        this.vehicleTitle = vehicleTitle;
    }
}
