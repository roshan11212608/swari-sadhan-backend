package swari.sewa.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight summary for the ShopOwner Dashboard.
 * Replaces 3 heavy API calls (500 vehicles, 20 enquiries, all reviews)
 * with a single response using count queries + 5-item paginated lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopDashboardSummaryDto {

    private VehicleCounts vehicleCounts;
    private EnquiryCounts enquiryCounts;
    private ReviewSummary reviewSummary;
    private List<RecentVehicle> recentVehicles;
    private List<RecentEnquiry> recentEnquiries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleCounts {
        private long available;
        private long published;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryCounts {
        private long pending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummary {
        private long count;
        private double averageRating;
        private List<RecentReview> recentReviews;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentVehicle {
        private Long id;
        private String title;
        private String brand;
        private String model;
        private String vehicleType;
        private BigDecimal sellPrice;
        private String mainImageUrl;
        private String status;
        private boolean sold;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentEnquiry {
        private Long id;
        private String status;
        private String customerName;
        private String vehicleTitle;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReview {
        private Long id;
        private String reviewerName;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
    }
}
