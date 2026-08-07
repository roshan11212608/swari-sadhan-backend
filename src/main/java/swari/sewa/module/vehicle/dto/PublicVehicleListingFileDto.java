package swari.sewa.module.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.PublicVehicleListingFileType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVehicleListingFileDto {
    private Long id;
    private String fileUrl;
    private String originalFilename;
    private PublicVehicleListingFileType fileType;
    private String documentType;
    private Boolean isPublic;
    private Boolean isCover;
    private Integer displayOrder;
}
