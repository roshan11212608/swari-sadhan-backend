package swari.sewa.module.expense.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swari.sewa.common.enums.ExpensePaymentMethod;
import swari.sewa.common.enums.ExpensePaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 255, message = "Title must be between 2 and 255 characters")
    private String title;
    
    @NotNull(message = "Category is required")
    private Long categoryId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;
    
    private String description;
    
    private String notes;
    
    private String vendorPaidTo;
    
    @NotNull(message = "Payment method is required")
    private ExpensePaymentMethod paymentMethod;
    
    @NotNull(message = "Payment status is required")
    private ExpensePaymentStatus paymentStatus;
    
    private String referenceNumber;
    
    private LocalDate dueDate;
}
