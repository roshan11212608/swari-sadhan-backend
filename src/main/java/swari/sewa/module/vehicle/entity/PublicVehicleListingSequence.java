package swari.sewa.module.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_vehicle_listing_sequence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicVehicleListingSequence {

    @Id
    @Column(name = "period_key", length = 6, nullable = false)
    private String yearMonth;

    @Column(name = "next_value", nullable = false)
    private long nextValue;
}
