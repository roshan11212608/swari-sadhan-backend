package swari.sewa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSearchRequest {
    private String keyword;
    
    private String brand;
    
    private String model;
    
    private String vehicleType;
    
    private String fuelType;
    
    private String transmissionType;
    
    private String bodyType;
    
    private String condition;
    
    private BigDecimal minPrice;
    
    private BigDecimal maxPrice;
    
    private Integer minYear;
    
    private Integer maxYear;
    
    private Integer minKilometers;
    
    private Integer maxKilometers;
    
    private String city;
    
    private String state;
    
    private Boolean isFeatured;
    
    private String sortBy;
    
    @Builder.Default
    private String sortDirection = "asc";
    
    @Builder.Default
    private Integer page = 1;
    
    @Builder.Default
    private Integer size = 10;
    
    // Manual getter for page
    public Integer getPage() {
        return page;
    }
    
    // Manual getters for search fields
    public Integer getSize() {
        return size;
    }
    
    public String getVehicleType() {
        return vehicleType;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public String getModel() {
        return model;
    }
    
    public String getFuelType() {
        return fuelType;
    }
    
    public BigDecimal getMinPrice() {
        return minPrice;
    }
    
    public BigDecimal getMaxPrice() {
        return maxPrice;
    }
    
    public Integer getMinYear() {
        return minYear;
    }
    
    public Integer getMaxYear() {
        return maxYear;
    }
    
    public Integer getMinKilometers() {
        return minKilometers;
    }
    
    public Integer getMaxKilometers() {
        return maxKilometers;
    }
}
