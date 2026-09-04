package swari.sewa.module.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lightweight summary of shop review statistics.
 * Replaces loading ALL reviews just to compute count, average, and distribution in JS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewSummaryDto {
    private long count;
    private double averageRating;
    private List<RatingBreakdown> distribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingBreakdown {
        private int star;
        private long count;
    }
}
