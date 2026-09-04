package swari.sewa.module.expense.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import swari.sewa.common.exception.ResourceNotFoundException;
import swari.sewa.module.expense.entity.Expense;
import swari.sewa.module.expense.entity.ExpenseCategory;
import swari.sewa.module.expense.repository.ExpenseRepository;
import swari.sewa.module.expense.service.impl.ExpenseServiceImpl;
import swari.sewa.module.shop.entity.Shop;
import swari.sewa.module.shop.repository.ShopRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression test for ExpenseServiceImpl N+1 fix.
 *
 * <p>Verifies that list endpoints use the JOIN FETCH repository variants
 * (findByShopIdAndIsActiveTrueWithDetails, searchExpensesWithDetails,
 * findByFiltersWithDetails, findUpcomingPaymentsWithDetails) instead of
 * the plain variants that trigger N+1 lazy loading on shop, category,
 * and attachments.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServicePerformanceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ShopRepository shopRepository;

    @InjectMocks private ExpenseServiceImpl expenseService;

    private void mockShopOwnership(String userEmail, Long shopId) {
        Shop shop = new Shop();
        shop.setId(shopId);
        when(shopRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(shop));
    }

    private Expense createExpense(Long id, Shop shop, ExpenseCategory category) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setShop(shop);
        expense.setCategory(category);
        expense.setAmount(new BigDecimal("100.00"));
        expense.setIsActive(true);
        return expense;
    }

    @Test
    void getExpensesByShop_usesJoinFetchVariant() {
        Long shopId = 1L;
        String email = "owner@test.com";
        mockShopOwnership(email, shopId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(Collections.emptyList());
        when(expenseRepository.findByShopIdAndIsActiveTrueWithDetails(shopId, pageable)).thenReturn(page);

        expenseService.getExpensesByShop(shopId, pageable, email);

        verify(expenseRepository).findByShopIdAndIsActiveTrueWithDetails(shopId, pageable);
        // Verify the old non-FETCH method is NOT called
        verify(expenseRepository, never()).findByShopIdAndIsActiveTrue(eq(shopId), any(Pageable.class));
    }

    @Test
    void searchExpenses_usesJoinFetchVariant() {
        Long shopId = 1L;
        String email = "owner@test.com";
        mockShopOwnership(email, shopId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(Collections.emptyList());
        when(expenseRepository.searchExpensesWithDetails(shopId, "test", pageable)).thenReturn(page);

        expenseService.searchExpenses(shopId, "test", pageable, email);

        verify(expenseRepository).searchExpensesWithDetails(shopId, "test", pageable);
        verify(expenseRepository, never()).searchExpenses(eq(shopId), anyString(), any(Pageable.class));
    }

    @Test
    void filterExpenses_usesJoinFetchVariant() {
        Long shopId = 1L;
        String email = "owner@test.com";
        mockShopOwnership(email, shopId);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(Collections.emptyList());
        when(expenseRepository.findByFiltersWithDetails(
                eq(shopId), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq(pageable)))
            .thenReturn(page);

        expenseService.filterExpenses(shopId, null, null, null, null, null, null, null, null, pageable, email);

        verify(expenseRepository).findByFiltersWithDetails(
                eq(shopId), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq(pageable));
        verify(expenseRepository, never()).findByFilters(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void getUpcomingPayments_usesJoinFetchVariant() {
        Long shopId = 1L;
        String email = "owner@test.com";
        mockShopOwnership(email, shopId);

        Pageable pageable = PageRequest.of(0, 3);
        when(expenseRepository.findUpcomingPaymentsWithDetails(shopId, pageable))
            .thenReturn(Collections.emptyList());

        expenseService.getUpcomingPayments(shopId, 3, email);

        verify(expenseRepository).findUpcomingPaymentsWithDetails(shopId, pageable);
        verify(expenseRepository, never()).findUpcomingPayments(anyLong(), any(Pageable.class));
    }

    @Test
    void getExpensesByShop_deniesAccessToWrongShop() {
        Long shopId = 2L;
        String email = "owner@test.com";
        // User owns shop 1, but requests shop 2
        Shop shop = new Shop();
        shop.setId(1L);
        when(shopRepository.findByUserEmail(email)).thenReturn(Optional.of(shop));

        assertThrows(ResourceNotFoundException.class, () ->
            expenseService.getExpensesByShop(shopId, PageRequest.of(0, 10), email));

        verify(expenseRepository, never()).findByShopIdAndIsActiveTrueWithDetails(anyLong(), any(Pageable.class));
    }
}
