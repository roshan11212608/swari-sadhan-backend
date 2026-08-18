package swari.sewa.module.auth.service;

import swari.sewa.module.auth.dto.ForgotPasswordRequest;
import swari.sewa.module.auth.dto.ResetPasswordRequest;

/**
 * Forgot-password flow: send an OTP to the user's email or mobile, then verify
 * the OTP and set a new password.
 */
public interface PasswordResetService {

    /**
     * Generate and email a 6-digit OTP for the given email. Throws if the email
     * is not registered.
     */
    String sendResetOtp(ForgotPasswordRequest request);

    /**
     * Verify the email OTP and set the new password. Updates the password in
     * both the {@code users} and {@code shop_owners} tables (whichever exist
     * for the email).
     */
    String verifyOtpAndResetPassword(ResetPasswordRequest request);

    /**
     * Generate and SMS a 6-digit OTP for the given mobile number. Throws if the
     * mobile is not registered. In dev mode the OTP is logged instead of sent
     * via paid SMS.
     */
    String sendResetOtpByMobile(String mobileNumber);

    /**
     * Verify the mobile OTP and set the new password. Looks up the user by
     * phone number and updates the password in the {@code users} table (and
     * {@code shop_owners} if a matching record exists).
     */
    String verifyMobileOtpAndResetPassword(String mobileNumber, String otp, String newPassword);
}
