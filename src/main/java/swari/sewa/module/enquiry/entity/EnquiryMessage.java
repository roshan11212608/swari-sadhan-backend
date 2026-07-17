package swari.sewa.module.enquiry.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.EnquiryMessageSender;

@Entity
@Table(name = "enquiry_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false)
    private EnquiryMessageSender sender;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
