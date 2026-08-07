package swari.sewa.module.vehicle.service;

import swari.sewa.module.vehicle.entity.Vehicle;

import java.math.BigDecimal;

/**
 * Vehicle Cost Calculator Interface
 * 
 * This abstraction prepares the architecture for future migration to a vehicle_expenses table.
 * Currently, vehicle costs are stored in the Vehicle entity (purchase_price, repair_cost, additional_expenses).
 * In the future, costs will be tracked in a separate vehicle_expenses table.
 * 
 * This interface ensures that when the migration happens, the change will be minimal
 * and only this implementation needs to change.
 */
public interface VehicleCostCalculator {
    
    /**
     * Calculate total cost of a vehicle
     * Total Cost = Purchase Price + Repair Cost + Additional Expenses
     * 
     * @param vehicle The vehicle entity
     * @return Total cost of the vehicle
     */
    BigDecimal calculateTotalCost(Vehicle vehicle);
    
    /**
     * Calculate COGS (Cost of Goods Sold) for a vehicle
     * COGS = Purchase Price + Repair Cost + Additional Expenses
     * 
     * @param vehicle The vehicle entity
     * @return COGS for the vehicle
     */
    BigDecimal calculateCOGS(Vehicle vehicle);
    
    /**
     * Get purchase price of a vehicle
     * 
     * @param vehicle The vehicle entity
     * @return Purchase price
     */
    BigDecimal getPurchasePrice(Vehicle vehicle);
    
    /**
     * Get repair cost of a vehicle
     * 
     * @param vehicle The vehicle entity
     * @return Repair cost
     */
    BigDecimal getRepairCost(Vehicle vehicle);
    
    /**
     * Get additional expenses of a vehicle
     * 
     * @param vehicle The vehicle entity
     * @return Additional expenses
     */
    BigDecimal getAdditionalExpenses(Vehicle vehicle);
    
    /**
     * Calculate gross profit for a vehicle
     * Gross Profit = Selling Price - COGS
     * 
     * @param vehicle The vehicle entity
     * @return Gross profit
     */
    BigDecimal calculateGrossProfit(Vehicle vehicle);
}
