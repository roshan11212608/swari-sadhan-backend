package swari.sewa.module.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated shop review response with summary stats.
 * Replaces loading ALL reviews + client-side computation with a single
 * server-side paginated + filtered response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewPageResponseDto {
    private List<ShopReviewDto> reviews;
    private ShopReviewSummaryDto summary;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
