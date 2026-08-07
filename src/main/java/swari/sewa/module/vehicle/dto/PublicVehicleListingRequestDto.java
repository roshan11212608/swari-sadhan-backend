package swari.sewa.module.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleListingRequestDto {

    private Long id;

    private String title;
    private String lotNumber;

    // Seller Information
    private String sellerName;
    private String sellerPhone;
    private String sellerAddress;

    // Owner Information
    private String ownerName;
    private String ownerPhone;
    private String ownerAddress;

    // Vehicle Information
    private String vehicleNumber;
    private String brand;
    private String model;
    private String variant;
    private Integer manufacturingYear;
    private Integer kilometersDriven;
    private String fuelType;
    private String engineCC;
    private String color;

    // Pricing
    private BigDecimal price;
    private String priceInWords;
    private Boolean negotiable;

    // Declaration
    private Boolean declarationAccepted;

    // Files
    private List<PublicVehicleListingFileDto> files;
}
