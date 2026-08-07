package swari.sewa.module.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.ExpensePaymentMethod;
import swari.sewa.common.enums.ExpensePaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String expenseNumber;
    private Long shopId;
    private String title;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;
    private String notes;
    private String vendorPaidTo;
    private ExpensePaymentMethod paymentMethod;
    private ExpensePaymentStatus paymentStatus;
    private String referenceNumber;
    private LocalDate dueDate;
    private String attachmentPath;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<ExpenseAttachmentResponse> attachments;
}
