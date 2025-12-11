package swari.sewa.common.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );
    
    private static final Pattern LICENSE_NUMBER_PATTERN = Pattern.compile(
            "^[A-Za-z0-9]{5,20}$"
    );
    
    private static final Pattern REGISTRATION_NUMBER_PATTERN = Pattern.compile(
            "^[A-Za-z]{2}[0-9]{2}[A-Za-z]{1,3}[0-9]{4}$"
    );
    
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    public boolean isValidLicenseNumber(String licenseNumber) {
        return licenseNumber != null && LICENSE_NUMBER_PATTERN.matcher(licenseNumber).matches();
    }
    
    public boolean isValidRegistrationNumber(String registrationNumber) {
        return registrationNumber != null && REGISTRATION_NUMBER_PATTERN.matcher(registrationNumber).matches();
    }
    
    public boolean isValidPrice(Double price) {
        return price != null && price > 0 && price <= 100000000; // Max 10 crore
    }
    
    public boolean isValidYear(Integer year) {
        return year != null && year >= 1900 && year <= java.time.Year.now().getValue() + 1;
    }
    
    public boolean isValidKilometers(Integer kilometers) {
        return kilometers != null && kilometers >= 0 && kilometers <= 1000000;
    }
    
    public boolean isValidEngineCapacity(String engineCapacity) {
        if (engineCapacity == null) return true;
        return engineCapacity.matches("^[0-9]{1,4}(\\.\\d{1,2})?\\s?(cc|CC)?$");
    }
    
    public boolean sanitizeString(String input) {
        if (input == null) return false;
        return !input.trim().isEmpty() && input.length() <= 500;
    }
    
    public boolean sanitizeDescription(String description) {
        if (description == null) return true;
        return description.length() <= 5000;
    }
}
