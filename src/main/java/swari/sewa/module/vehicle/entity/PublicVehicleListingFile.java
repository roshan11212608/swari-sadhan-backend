package swari.sewa.module.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import swari.sewa.common.enums.PublicVehicleListingFileType;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "public_vehicle_listing_files",
    indexes = {
        @Index(name = "idx_pvlf_listing", columnList = "listing_id"),
        @Index(name = "idx_pvlf_file_type", columnList = "file_type"),
        @Index(name = "idx_pvlf_is_public", columnList = "is_public")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicVehicleListingFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private PublicVehicleListing listing;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private PublicVehicleListingFileType fileType;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = false;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
