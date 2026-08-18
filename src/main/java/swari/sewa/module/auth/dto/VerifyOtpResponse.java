package swari.sewa.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpResponse {
    private String message;
    /** Single-use, short-lived token proving the mobile number was verified. */
    private String signupVerificationToken;
    private String maskedMobile;
}
