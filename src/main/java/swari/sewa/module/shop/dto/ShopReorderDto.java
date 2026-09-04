package swari.sewa.module.shop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopReorderDto {
    @NotNull(message = "Shop id is required")
    private Long id;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be 0 or greater")
    private Integer displayOrder;
}
