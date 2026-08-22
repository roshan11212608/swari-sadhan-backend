package swari.sewa.module.subscription.service;

import java.math.BigDecimal;

public interface SubscriptionLimitService {
    boolean canAddVehicle(Long shopOwnerId);
    boolean canAddEmployee(Long shopOwnerId);
    Long getVehicleCount(Long shopOwnerId);
    Long getEmployeeCount(Long shopOwnerId);
    void validateVehicleLimit(Long shopOwnerId);
    void validateEmployeeLimit(Long shopOwnerId);
}
