package swari.sewa.common.constants;

/**
 * Date Filter Constants for Analytics
 * Centralized constants for date filter types
 */
public class DateFilter {
    
    public static final String TODAY = "today";
    public static final String YESTERDAY = "yesterday";
    public static final String THIS_WEEK = "thisweek";
    public static final String THIS_MONTH = "thismonth";
    public static final String LAST_MONTH = "lastmonth";
    public static final String THIS_YEAR = "thisyear";
    
    // Valid date filter values
    public static final String[] VALID_FILTERS = {
        TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR
    };
    
    /**
     * Check if a filter value is valid
     */
    public static boolean isValid(String filter) {
        if (filter == null) return false;
        for (String validFilter : VALID_FILTERS) {
            if (validFilter.equalsIgnoreCase(filter)) {
                return true;
            }
        }
        return false;
    }
    
    private DateFilter() {
        // Utility class - prevent instantiation
    }
}
