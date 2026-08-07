package swari.sewa.module.analytics.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for chart data processing
 * Handles zero-filling for missing periods in trend data
 */
public class ChartDataUtil {

    /**
     * Fill missing months with zero values in trend data
     * 
     * @param trendData List of Object[] where each row is [period, value1, value2, ...]
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @param isYearly Whether grouping is yearly
     * @param valueIndices Array of indices in each row that contain values to fill
     * @return List of Object[] with all periods filled (missing periods have 0 values)
     */
    public static List<Object[]> fillMissingPeriods(
            List<Object[]> trendData, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            boolean isYearly,
            int[] valueIndices) {
        
        List<String> allPeriods = generateAllPeriods(startDate, endDate, isYearly);
        Map<String, Object[]> dataByPeriod = new HashMap<>();
        
        // Build map of existing data
        for (Object[] row : trendData) {
            String period = (String) row[0];
            dataByPeriod.put(period, row);
        }
        
        // Build result with all periods
        List<Object[]> result = new ArrayList<>();
        for (String period : allPeriods) {
            if (dataByPeriod.containsKey(period)) {
                result.add(dataByPeriod.get(period));
            } else {
                // Create row with zero values
                Object[] zeroRow = new Object[rowLength(trendData, valueIndices.length)];
                zeroRow[0] = period;
                for (int i = 0; i < valueIndices.length; i++) {
                    zeroRow[valueIndices[i]] = BigDecimal.ZERO;
                }
                result.add(zeroRow);
            }
        }
        
        return result;
    }
    
    /**
     * Generate all periods (months or years) for a date range
     */
    private static List<String> generateAllPeriods(LocalDateTime startDate, LocalDateTime endDate, boolean isYearly) {
        List<String> periods = new ArrayList<>();
        
        if (isYearly) {
            // Generate years
            int startYear = startDate.getYear();
            int endYear = endDate.getYear();
            for (int year = startYear; year <= endYear; year++) {
                periods.add(String.valueOf(year));
            }
        } else {
            // Generate months
            YearMonth start = YearMonth.from(startDate);
            YearMonth end = YearMonth.from(endDate);
            
            YearMonth current = start;
            while (!current.isAfter(end)) {
                periods.add(current.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
                current = current.plusMonths(1);
            }
        }
        
        return periods;
    }
    
    /**
     * Determine row length based on value indices
     */
    private static int rowLength(List<Object[]> trendData, int valueCount) {
        if (trendData.isEmpty()) {
            return valueCount + 1; // period + values
        }
        return trendData.get(0).length;
    }
    
    /**
     * Fill missing months for simple trend data (period + single value)
     */
    public static List<Object[]> fillMissingPeriods(
            List<Object[]> trendData, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            boolean isYearly) {
        return fillMissingPeriods(trendData, startDate, endDate, isYearly, new int[]{1});
    }
    
    /**
     * Fill missing months for trend data with two values (period + value1 + value2)
     */
    public static List<Object[]> fillMissingPeriodsTwoValues(
            List<Object[]> trendData, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            boolean isYearly) {
        return fillMissingPeriods(trendData, startDate, endDate, isYearly, new int[]{1, 2});
    }
    
    /**
     * Fill missing months for trend data with three values (period + value1 + value2 + value3)
     */
    public static List<Object[]> fillMissingPeriodsThreeValues(
            List<Object[]> trendData, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            boolean isYearly) {
        return fillMissingPeriods(trendData, startDate, endDate, isYearly, new int[]{1, 2, 3});
    }
}
