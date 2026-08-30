package swari.sewa.module.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shop_owner_remarks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopOwnerRemark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_owner_id", nullable = false)
    private Long shopOwnerId;

    @Column(name = "remark", nullable = false, columnDefinition = "TEXT")
    private String remark;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
