package swari.sewa.module.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.dto.LoginRequest;
import swari.sewa.common.dto.LoginResponse;
import swari.sewa.common.dto.SignupRequest;
import swari.sewa.module.auth.dto.MobileLoginRequest;
import swari.sewa.module.auth.dto.OtpMessageResponse;
import swari.sewa.module.auth.dto.OtpSignupCompleteRequest;
import swari.sewa.module.auth.dto.RefreshTokenRequest;
import swari.sewa.module.auth.dto.SendOtpRequest;
import swari.sewa.module.auth.dto.ShopRegSendOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpRequest;
import swari.sewa.module.auth.dto.ShopRegVerifyOtpResponse;
import swari.sewa.module.auth.dto.VerifyOtpRequest;
import swari.sewa.module.auth.dto.VerifyOtpResponse;
import swari.sewa.module.auth.dto.ForgotPasswordRequest;
import swari.sewa.module.auth.dto.ResetPasswordRequest;
import swari.sewa.module.auth.service.AuthService;
import swari.sewa.module.auth.service.OtpSignupService;
import swari.sewa.module.auth.service.PasswordResetService;
import swari.sewa.module.auth.service.ShopRegistrationService;
import swari.sewa.module.user.dto.UserDto;
import swari.sewa.module.dashboard.dto.ShopOwnerDto;
import swari.sewa.module.user.service.AdminShopOwnerService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AdminShopOwnerService adminShopOwnerService;
    private final OtpSignupService otpSignupService;
    private final ShopRegistrationService shopRegistrationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register-shop")
    public ResponseEntity<ShopOwnerDto> registerShop(@Valid @RequestBody ShopOwnerDto shopOwnerDto) {
        ShopOwnerDto created = adminShopOwnerService.createShopOwner(shopOwnerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/register-shop-with-files")
    public ResponseEntity<ShopOwnerDto> registerShopWithFiles(
            @RequestParam("shopOwnerData") String shopOwnerDataJson,
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
            @RequestParam(value = "shopLogo", required = false) MultipartFile shopLogo,
            @RequestParam(value = "citizenshipPicFront", required = false) MultipartFile citizenshipPicFront,
            @RequestParam(value = "citizenshipPicBack", required = false) MultipartFile citizenshipPicBack,
            @RequestParam(value = "shopRegUpload", required = false) MultipartFile shopRegUpload) {
        ShopOwnerDto created = adminShopOwnerService.createShopOwnerWithFiles(
                shopOwnerDataJson, profilePhoto, shopLogo, citizenshipPicFront, citizenshipPicBack, shopRegUpload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserDto userDto = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    // ------------------------------------------------------------------
    // OTP-based public signup flow
    // ------------------------------------------------------------------

    @PostMapping("/signup/send-otp")
    public ResponseEntity<OtpMessageResponse> sendSignupOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(otpSignupService.sendOtp(request.getMobileNumber()));
    }

    @PostMapping("/signup/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifySignupOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(otpSignupService.verifyOtp(request));
    }

    @PostMapping("/signup/complete")
    public ResponseEntity<UserDto> completeOtpSignup(@Valid @RequestBody OtpSignupCompleteRequest request) {
        UserDto created = otpSignupService.completeSignup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ------------------------------------------------------------------
    // Shop registration OTP flow (email + mobile verification)
    // ------------------------------------------------------------------

    @PostMapping("/shop-reg/send-otp")
    public ResponseEntity<OtpMessageResponse> sendShopRegOtp(@Valid @RequestBody ShopRegSendOtpRequest request) {
        String message = shopRegistrationService.sendOtps(request);
        return ResponseEntity.ok(OtpMessageResponse.builder().message(message).build());
    }

    @PostMapping("/shop-reg/verify-otp")
    public ResponseEntity<ShopRegVerifyOtpResponse> verifyShopRegOtp(@Valid @RequestBody ShopRegVerifyOtpRequest request) {
        return ResponseEntity.ok(shopRegistrationService.verifyOtps(request));
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/login/mobile")
    public ResponseEntity<LoginResponse> loginWithMobile(@Valid @RequestBody MobileLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithMobile(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.emailExists(email));
    }

    // ------------------------------------------------------------------
    // Forgot password flow (email OTP → verify + reset)
    // ------------------------------------------------------------------

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.sendResetOtp(request);
        return ResponseEntity.ok(OtpMessageResponse.builder().message(message).build());
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<OtpMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = passwordResetService.verifyOtpAndResetPassword(request);
        return ResponseEntity.ok(OtpMessageResponse.builder().message(message).build());
    }

    // ------------------------------------------------------------------
    // Forgot password via mobile (for public users without email)
    // ------------------------------------------------------------------

    @PostMapping("/forgot-password/mobile/send")
    public ResponseEntity<OtpMessageResponse> forgotPasswordMobileSend(@RequestParam String mobileNumber) {
        String message = passwordResetService.sendResetOtpByMobile(mobileNumber);
        return ResponseEntity.ok(OtpMessageResponse.builder().message(message).build());
    }

    @PostMapping("/forgot-password/mobile/reset")
    public ResponseEntity<OtpMessageResponse> forgotPasswordMobileReset(
            @RequestParam String mobileNumber,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        String message = passwordResetService.verifyMobileOtpAndResetPassword(mobileNumber, otp, newPassword);
        return ResponseEntity.ok(OtpMessageResponse.builder().message(message).build());
    }
}
