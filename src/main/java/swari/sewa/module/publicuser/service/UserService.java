package swari.sewa.module.publicuser.service;

import java.util.List;
import java.util.Optional;

import swari.sewa.common.dto.LoginRequest;
import swari.sewa.common.dto.LoginResponse;
import swari.sewa.common.dto.SignupRequest;
import swari.sewa.module.publicuser.dto.UserDto;
import swari.sewa.common.enums.UserRole;

public interface UserService {
    
    UserDto createUser(SignupRequest signupRequest);
    
    Optional<UserDto> getUserById(Long id);
    
    Optional<UserDto> getUserByEmail(String email);
    
    List<UserDto> getAllUsers();
    
    List<UserDto> getUsersByRole(UserRole role);
    
    UserDto updateUser(Long id, UserDto userDto);
    
    void deleteUser(Long id);
    
    UserDto activateUser(Long id);
    
    UserDto deactivateUser(Long id);
    
    LoginResponse authenticateUser(LoginRequest loginRequest);
    
    boolean existsByEmail(String email);
    
    UserDto changePassword(Long userId, String currentPassword, String newPassword);
    
    UserDto resetPassword(String email);
}
