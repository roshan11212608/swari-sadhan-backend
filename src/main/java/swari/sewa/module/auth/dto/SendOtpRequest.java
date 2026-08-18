package swari.sewa.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^\\+?9779[6-8]\\d{8}$|^\\+?9[6-8]\\d{8}$",
            message = "Enter a valid Nepal mobile number (e.g. +97798XXXXXXXX)"
    )
    private String mobileNumber;
}
