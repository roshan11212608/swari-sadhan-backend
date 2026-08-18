package swari.sewa.module.auth.service;

import swari.sewa.module.auth.dto.OtpMessageResponse;
import swari.sewa.module.auth.dto.OtpSignupCompleteRequest;
import swari.sewa.module.auth.dto.VerifyOtpRequest;
import swari.sewa.module.auth.dto.VerifyOtpResponse;
import swari.sewa.module.user.dto.UserDto;

public interface OtpSignupService {

    /** Generate and send a new OTP to the given mobile number. */
    OtpMessageResponse sendOtp(String mobileNumber);

    /** Validate the OTP and issue a single-use verification token. */
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    /** Create the public user account after verifying the token server-side. */
    UserDto completeSignup(OtpSignupCompleteRequest request);
}
