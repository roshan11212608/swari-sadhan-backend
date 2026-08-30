package swari.sewa.common.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Single source of truth for business/reporting date arithmetic.
 *
 * <p>Database timestamps are stored in UTC ({@code serverTimezone=UTC} on the
 * JDBC URL, {@code spring.jackson.time-zone=UTC}). Reporting periods, however,
 * must follow the business timezone — a Nepali merchant expects "this month" to
 * mean the calendar month in Nepal, not in UTC.
 *
 * <p>Never use {@code LocalDateTime.now()} directly in reporting code: it silently
 * depends on the JVM default timezone, which differs between developer machines,
 * CI and production containers.
 *
 * <p>Configured via {@code app.reporting.timezone} (default {@code Asia/Kathmandu}).
 */
@Component
public class ReportingClock {

    @Getter
    private final ZoneId zone;

    public ReportingClock(@Value("${app.reporting.timezone:Asia/Kathmandu}") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    /** Current instant expressed in the business timezone. */
    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(zone);
    }

    /**
     * Current instant as a {@code LocalDateTime} in the business timezone, for
     * use in JPA queries against {@code DATETIME} columns.
     */
    public LocalDateTime now() {
        return nowZoned().toLocalDateTime();
    }

    /** First moment of the current calendar month in the business timezone. */
    public LocalDateTime startOfCurrentMonth() {
        return nowZoned().withDayOfMonth(1).toLocalDate().atStartOfDay();
    }

    /** First moment of the current calendar year in the business timezone. */
    public LocalDateTime startOfCurrentYear() {
        return nowZoned().withDayOfYear(1).toLocalDate().atStartOfDay();
    }

    /**
     * Resolve a dashboard time-range token to an inclusive start boundary.
     *
     * <p>Semantics are defined once here so backend and frontend cannot drift:
     * <pre>
     *   7d  = start of day, 6 days ago  -> today plus the previous 6 days  (7 calendar days)
     *   30d = start of day, 29 days ago -> 30 calendar days including today
     *   90d = start of day, 89 days ago -> 90 calendar days including today
     *   1y  = start of the month 11 months ago -> 12 calendar months including this one
     * </pre>
     * Boundaries snap to start-of-day (and start-of-month for {@code 1y}) so that
     * repeated calls within the same day return identical, cacheable windows
     * instead of a sliding sub-second window.
     */
    public LocalDateTime resolveRangeStart(String timeRange) {
        ZonedDateTime now = nowZoned();
        String range = timeRange == null ? "30d" : timeRange.trim().toLowerCase();
        return switch (range) {
            case "7d" -> now.minusDays(6).toLocalDate().atStartOfDay();
            case "90d" -> now.minusDays(89).toLocalDate().atStartOfDay();
            case "1y" -> now.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "30d" -> now.minusDays(29).toLocalDate().atStartOfDay();
            default -> now.minusDays(29).toLocalDate().atStartOfDay();
        };
    }
}
