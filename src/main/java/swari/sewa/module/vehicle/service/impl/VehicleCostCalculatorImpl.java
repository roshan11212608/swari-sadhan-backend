package swari.sewa.module.vehicle.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swari.sewa.module.vehicle.entity.Vehicle;
import swari.sewa.module.vehicle.service.VehicleCostCalculator;

import java.math.BigDecimal;

/**
 * Vehicle Cost Calculator Implementation
 * 
 * Current implementation uses Vehicle entity fields (purchase_price, repair_cost, additional_expenses).
 * 
 * Future implementation will query from vehicle_expenses table.
 * Only this class needs to change when migrating to vehicle_expenses table.
 */
@Service
@Slf4j
public class VehicleCostCalculatorImpl implements VehicleCostCalculator {

    @Override
    public BigDecimal calculateTotalCost(Vehicle vehicle) {
        if (vehicle == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal purchasePrice = vehicle.getPurchasePrice() != null ? vehicle.getPurchasePrice() : BigDecimal.ZERO;
        BigDecimal repairCost = vehicle.getRepairCost() != null ? vehicle.getRepairCost() : BigDecimal.ZERO;
        BigDecimal additionalExpenses = vehicle.getAdditionalExpenses() != null ? vehicle.getAdditionalExpenses() : BigDecimal.ZERO;
        
        return purchasePrice.add(repairCost).add(additionalExpenses);
    }

    @Override
    public BigDecimal calculateCOGS(Vehicle vehicle) {
        // COGS is the same as total cost for vehicles
        return calculateTotalCost(vehicle);
    }

    @Override
    public BigDecimal getPurchasePrice(Vehicle vehicle) {
        if (vehicle == null || vehicle.getPurchasePrice() == null) {
            return BigDecimal.ZERO;
        }
        return vehicle.getPurchasePrice();
    }

    @Override
    public BigDecimal getRepairCost(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRepairCost() == null) {
            return BigDecimal.ZERO;
        }
        return vehicle.getRepairCost();
    }

    @Override
    public BigDecimal getAdditionalExpenses(Vehicle vehicle) {
        if (vehicle == null || vehicle.getAdditionalExpenses() == null) {
            return BigDecimal.ZERO;
        }
        return vehicle.getAdditionalExpenses();
    }

    @Override
    public BigDecimal calculateGrossProfit(Vehicle vehicle) {
        if (vehicle == null || vehicle.getSellingPrice() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sellingPrice = vehicle.getSellingPrice();
        BigDecimal cogs = calculateCOGS(vehicle);
        
        return sellingPrice.subtract(cogs);
    }
}
