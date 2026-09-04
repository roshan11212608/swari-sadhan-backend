package swari.sewa.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swari.sewa.module.enquiry.repository.EnquiryRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnquirySecurity query optimization.
 *
 * <p>Verifies that ownership checks use lightweight projection queries
 * (findCustomerEmailById, findShopOwnerEmailById) instead of loading
 * the full Enquiry entity and its lazy customer/shop/shopOwner relationships.
 */
@ExtendWith(MockitoExtension.class)
class EnquirySecurityTest {

    @Mock private EnquiryRepository enquiryRepository;

    @InjectMocks
    private EnquirySecurity enquirySecurity;

    @Test
    void isCustomer_usesProjectionQuery_notFindById() {
        when(enquiryRepository.findCustomerEmailById(eq(1L))).thenReturn(Optional.of("customer@test.com"));

        boolean result = enquirySecurity.isCustomer(1L, "customer@test.com");

        assertTrue(result);
        verify(enquiryRepository).findCustomerEmailById(eq(1L));
        verify(enquiryRepository, never()).findById(anyLong());
    }

    @Test
    void isCustomer_returnsFalseWhenEmailDoesNotMatch() {
        when(enquiryRepository.findCustomerEmailById(eq(1L))).thenReturn(Optional.of("other@test.com"));

        boolean result = enquirySecurity.isCustomer(1L, "customer@test.com");

        assertFalse(result);
    }

    @Test
    void isCustomer_returnsFalseWhenEnquiryNotFound() {
        when(enquiryRepository.findCustomerEmailById(eq(1L))).thenReturn(Optional.empty());

        boolean result = enquirySecurity.isCustomer(1L, "customer@test.com");

        assertFalse(result);
    }

    @Test
    void isCustomer_returnsFalseWhenEnquiryIdIsNull() {
        boolean result = enquirySecurity.isCustomer(null, "customer@test.com");

        assertFalse(result);
        verify(enquiryRepository, never()).findCustomerEmailById(any());
    }

    @Test
    void isCustomer_returnsFalseWhenEmailIsNull() {
        boolean result = enquirySecurity.isCustomer(1L, null);

        assertFalse(result);
        verify(enquiryRepository, never()).findCustomerEmailById(any());
    }

    @Test
    void isShopOwner_usesProjectionQuery_notFindById() {
        when(enquiryRepository.findShopOwnerEmailById(eq(1L))).thenReturn(Optional.of("owner@test.com"));

        boolean result = enquirySecurity.isShopOwner(1L, "owner@test.com");

        assertTrue(result);
        verify(enquiryRepository).findShopOwnerEmailById(eq(1L));
        verify(enquiryRepository, never()).findById(anyLong());
    }

    @Test
    void isShopOwner_returnsFalseWhenEmailDoesNotMatch() {
        when(enquiryRepository.findShopOwnerEmailById(eq(1L))).thenReturn(Optional.of("other@test.com"));

        boolean result = enquirySecurity.isShopOwner(1L, "owner@test.com");

        assertFalse(result);
    }

    @Test
    void isShopOwner_returnsFalseWhenEnquiryNotFound() {
        when(enquiryRepository.findShopOwnerEmailById(eq(1L))).thenReturn(Optional.empty());

        boolean result = enquirySecurity.isShopOwner(1L, "owner@test.com");

        assertFalse(result);
    }

    @Test
    void isShopOwner_returnsFalseWhenEnquiryIdIsNull() {
        boolean result = enquirySecurity.isShopOwner(null, "owner@test.com");

        assertFalse(result);
        verify(enquiryRepository, never()).findShopOwnerEmailById(any());
    }
}
