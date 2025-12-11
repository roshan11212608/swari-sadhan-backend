package swari.sewa.module.publicuser.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swari.sewa.module.publicuser.dto.UserDto;
import swari.sewa.common.enums.UserRole;
import swari.sewa.module.publicuser.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('SHOP_OWNER') and @userSecurity.isOwner(#id, authentication.name)) or (hasRole('CUSTOMER') and @userSecurity.isOwner(#id, authentication.name))")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRole role) {
        List<UserDto> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('SHOP_OWNER') and @userSecurity.isOwner(#id, authentication.name)) or (hasRole('CUSTOMER') and @userSecurity.isOwner(#id, authentication.name))")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> activateUser(@PathVariable Long id) {
        UserDto user = userService.activateUser(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> deactivateUser(@PathVariable Long id) {
        UserDto user = userService.deactivateUser(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/change-password")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('SHOP_OWNER') and @userSecurity.isOwner(#id, authentication.name)) or (hasRole('CUSTOMER') and @userSecurity.isOwner(#id, authentication.name))")
    public ResponseEntity<UserDto> changePassword(
            @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        UserDto user = userService.changePassword(id, currentPassword, newPassword);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> resetPassword(@RequestParam String email) {
        UserDto user = userService.resetPassword(email);
        return ResponseEntity.ok(user);
    }
}
