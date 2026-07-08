package swari.sewa.module.vehicle.dto;

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
public class SellApplicationDto {
    private Long id;
    private Long vehicleId;
    private String customerName;
    private String customerParentName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String customerCitizenshipNumber;
    private String citizenshipFrontPhoto;
    private String citizenshipBackPhoto;
    private String customerPhoto;
    private LocalDateTime applicationDate;
    private BigDecimal offeredPrice;
    private String offeredPriceInWords;
    private String paymentMethod;
    private BigDecimal downPayment;
    private Boolean financingRequired;
    private String financingBank;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
