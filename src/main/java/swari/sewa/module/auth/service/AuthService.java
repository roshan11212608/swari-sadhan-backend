package swari.sewa.module.auth.service;

import swari.sewa.common.dto.LoginRequest;
import swari.sewa.common.dto.LoginResponse;
import swari.sewa.common.dto.SignupRequest;
import swari.sewa.module.auth.dto.MobileLoginRequest;
import swari.sewa.module.user.dto.UserDto;

public interface AuthService {

    UserDto signup(SignupRequest signupRequest);

    LoginResponse login(LoginRequest loginRequest);

    LoginResponse loginWithMobile(MobileLoginRequest request);

    LoginResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    boolean emailExists(String email);
}
