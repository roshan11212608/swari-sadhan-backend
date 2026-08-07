package swari.sewa.module.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "public_vehicle_listing_review_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicVehicleListingReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    /** Who performed the action: ADMIN or SELLER */
    @Column(name = "actor", nullable = false, length = 20)
    private String actor;

    /** The action taken: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, CHANGES_REQUESTED, SOLD, SELLER_UPDATED */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
