package swari.sewa.module.homepage.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class HomeBudgetDto {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Max price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Max price must be greater than 0")
    private BigDecimal maxPrice;

    @NotBlank(message = "Image is required")
    private String imageUrl;

    private Boolean isActive;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
