package swari.sewa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    
    private String refreshToken;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private Long userId;
    
    private String email;
    
    private String firstName;
    
    private String lastName;
    
    private String role;
    
    private Long shopId; // For shop owners
    
    private String phone; // User's phone number
}
