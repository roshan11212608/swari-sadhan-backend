package swari.sewa.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("financeDashboard"),
            new ConcurrentMapCache("financeIncome"),
            new ConcurrentMapCache("financeExpenses"),
            new ConcurrentMapCache("financeProfit"),
            new ConcurrentMapCache("financeCashFlow"),
            new ConcurrentMapCache("financeOutstanding"),
            new ConcurrentMapCache("vehicleInvestment"),
            new ConcurrentMapCache("paymentSummary"),
            new ConcurrentMapCache("analyticsDashboard"),
            new ConcurrentMapCache("analyticsStock"),
            new ConcurrentMapCache("analyticsPeriod"),
            new ConcurrentMapCache("analyticsProfitability"),
            new ConcurrentMapCache("analyticsAverage"),
            new ConcurrentMapCache("analyticsInventory")
        ));
        return cacheManager;
    }
}
