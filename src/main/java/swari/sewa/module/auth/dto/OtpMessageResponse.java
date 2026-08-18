package swari.sewa.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpMessageResponse {
    private String message;
    /** Seconds the user must wait before requesting another OTP. */
    private int resendCooldownSeconds;
    /** Masked mobile number for display, e.g. +977 98******12 */
    private String maskedMobile;
}
