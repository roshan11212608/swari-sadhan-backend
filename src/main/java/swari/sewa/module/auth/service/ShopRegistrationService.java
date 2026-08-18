package swari.sewa.module.auth.service;

import swari.sewa.module.auth.dto.ShopRegSendOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpResponse;

public interface ShopRegistrationService {

    /**
     * Send OTPs to the shop owner's email and mobile number.
     */
    String sendOtps(ShopRegSendOtpRequest request);

    /**
     * Verify both email and mobile OTPs. Returns a verification token that
     * must be presented when submitting the full registration.
     */
    ShopRegVerifyOtpResponse verifyOtps(ShopRegVerifyOtpRequest request);
}
