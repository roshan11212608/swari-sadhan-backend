package swari.sewa.module.expense.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.dto.ApiResponse;
import swari.sewa.common.service.FileStorageService;
import swari.sewa.module.expense.dto.ExpenseDashboardResponse;
import swari.sewa.module.expense.dto.ExpenseRequest;
import swari.sewa.module.expense.dto.ExpenseResponse;
import swari.sewa.module.expense.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExpenseController {

    private final ExpenseService expenseService;
    private final FileStorageService fileStorageService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody ExpenseRequest expenseRequest,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ExpenseResponse expense = expenseService.createExpense(expenseRequest, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expense, "Expense created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseById(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Optional<ExpenseResponse> expense = expenseService.getExpenseById(id, userEmail);
        return expense.map(e -> ResponseEntity.ok(ApiResponse.success(e)))
                      .orElse(ResponseEntity.ok(ApiResponse.error("Expense not found")));
    }

    @GetMapping("/number/{expenseNumber}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpenseByNumber(
            @PathVariable String expenseNumber,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Optional<ExpenseResponse> expense = expenseService.getExpenseByNumber(expenseNumber, userEmail);
        return expense.map(e -> ResponseEntity.ok(ApiResponse.success(e)))
                      .orElse(ResponseEntity.ok(ApiResponse.error("Expense not found")));
    }

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByShop(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseResponse> expenses = expenseService.getExpensesByShop(shopId, pageable, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam Long shopId,
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseResponse> expenses = expenseService.searchExpenses(shopId, searchTerm, pageable, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> filterExpenses(
            @RequestParam Long shopId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        String userEmail = authentication.getName();
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExpenseResponse> expenses = expenseService.filterExpenses(
                shopId, title, categoryId, paymentStatus, paymentMethod,
                startDate, endDate, minAmount, maxAmount, pageable, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest expenseRequest,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ExpenseResponse expense = expenseService.updateExpense(id, expenseRequest, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expense, "Expense updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @CacheEvict(value = "analyticsDashboard", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        expenseService.deleteExpense(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    @GetMapping("/recent/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getRecentExpenses(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        String userEmail = authentication.getName();
        List<ExpenseResponse> expenses = expenseService.getRecentExpenses(shopId, limit, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/upcoming/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> getUpcomingPayments(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "3") int limit,
            Authentication authentication) {
        String userEmail = authentication.getName();
        List<ExpenseResponse> expenses = expenseService.getUpcomingPayments(shopId, limit, userEmail);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/today/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getTodayExpenses(
            @PathVariable Long shopId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BigDecimal total = expenseService.getTodayExpenses(shopId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/pending/{shopId}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<BigDecimal>> getPendingPayments(
            @PathVariable Long shopId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        BigDecimal total = expenseService.getPendingPayments(shopId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        log.info("=== UPLOAD ATTACHMENT ENDPOINT CALLED ===");
        log.info("Expense ID: {}", id);
        log.info("File Name: {}", file.getOriginalFilename());
        log.info("File Size: {}", file.getSize());
        log.info("Content Type: {}", file.getContentType());
        
        String userEmail = authentication.getName();
        log.info("User Email: {}", userEmail);
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            log.error("Invalid file type: {}", contentType);
            return ResponseEntity.ok(ApiResponse.error("Only images and PDF files are allowed"));
        }
        
        // Validate file size (10MB max)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.error("File size exceeds limit: {}", file.getSize());
            return ResponseEntity.ok(ApiResponse.error("File size exceeds 10MB limit"));
        }
        
        try {
            String filePath = fileStorageService.storeFile(file);
            log.info("File stored at: {}", filePath);
            
            // Create attachment record in database
            expenseService.addAttachment(id, file.getOriginalFilename(), filePath, file.getSize(), contentType, userEmail);
            
            return ResponseEntity.ok(ApiResponse.success(Map.of("fileUrl", filePath), "File uploaded successfully"));
        } catch (Exception e) {
            log.error("Error uploading attachment", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to upload attachment: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<ApiResponse<ExpenseDashboardResponse>> getDashboardSummary(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer trendMonths,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ExpenseDashboardResponse dashboard = expenseService.getDashboardSummary(userEmail, period, startDate, endDate, trendMonths);
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard data retrieved successfully"));
    }
}
