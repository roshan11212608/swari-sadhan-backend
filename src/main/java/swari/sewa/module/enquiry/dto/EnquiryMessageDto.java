package swari.sewa.module.enquiry.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.EnquiryMessageSender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnquiryMessageDto {
    private Long id;
    private Long enquiryId;
    private EnquiryMessageSender sender;
    private String senderName;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
