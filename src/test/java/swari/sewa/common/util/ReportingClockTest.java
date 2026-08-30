package swari.sewa.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReportingClock}.
 *
 * Verifies the business rule that all reporting boundaries are computed in the
 * configured business timezone (default Asia/Kathmandu) and never from the JVM
 * default timezone, and that the dashboard time-range tokens resolve to fixed,
 * cacheable windows:
 *
 *   7d  = start of day, 6 days ago   (7 calendar days including today)
 *   30d = start of day, 29 days ago  (30 calendar days including today)
 *   90d = start of day, 89 days ago  (90 calendar days including today)
 *   1y  = start of the month 11 months ago (12 calendar months including this one)
 *   null / unknown token -> same as 30d
 *
 * Boundaries snap to start-of-day (and start-of-month for 1y), so repeated calls
 * within the same day must return identical values rather than a sliding
 * sub-second window.
 */
class ReportingClockTest {

    private static final String KATHMANDU = "Asia/Kathmandu";

    private final ReportingClock clock = new ReportingClock(KATHMANDU);

    /** Today's date as the business timezone sees it. */
    private java.time.LocalDate businessToday() {
        return ZonedDateTime.now(ZoneId.of(KATHMANDU)).toLocalDate();
    }

    @Test
    @DisplayName("Zone is Asia/Kathmandu when constructed with that string")
    void testZone_kathmandu() {
        assertEquals(ZoneId.of(KATHMANDU), clock.getZone());
    }

    @Test
    @DisplayName("A different configured zone (UTC) is honoured")
    void testZone_utcHonoured() {
        ReportingClock utcClock = new ReportingClock("UTC");
        assertEquals(ZoneId.of("UTC"), utcClock.getZone());
        assertNotEquals(clock.getZone(), utcClock.getZone());
    }

    @Test
    @DisplayName("startOfCurrentMonth() is the 1st of the month at exactly 00:00:00.000")
    void testStartOfCurrentMonth() {
        LocalDateTime start = clock.startOfCurrentMonth();
        assertEquals(1, start.getDayOfMonth(), "must be the first day of the month");
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
        assertEquals(0, start.getSecond());
        assertEquals(0, start.getNano());
        assertEquals(businessToday().getMonth(), start.getMonth());
        assertEquals(businessToday().getYear(), start.getYear());
    }

    @Test
    @DisplayName("startOfCurrentYear() is 1 January at exactly 00:00:00.000")
    void testStartOfCurrentYear() {
        LocalDateTime start = clock.startOfCurrentYear();
        assertEquals(1, start.getDayOfYear(), "must be the first day of the year");
        assertEquals(Month.JANUARY, start.getMonth());
        assertEquals(1, start.getDayOfMonth());
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
        assertEquals(0, start.getSecond());
        assertEquals(0, start.getNano());
        assertEquals(businessToday().getYear(), start.getYear());
    }

    @Test
    @DisplayName("resolveRangeStart(\"7d\") snaps to start of day (00:00:00.000)")
    void testResolve7d_startOfDay() {
        LocalDateTime start = clock.resolveRangeStart("7d");
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
        assertEquals(0, start.getSecond());
        assertEquals(0, start.getNano());
    }

    @Test
    @DisplayName("resolveRangeStart(\"7d\") is 6 days before today in the business timezone")
    void testResolve7d_sixDaysBack() {
        java.time.LocalDate expected = ZonedDateTime.now(ZoneId.of(KATHMANDU)).minusDays(6).toLocalDate();
        assertEquals(expected, clock.resolveRangeStart("7d").toLocalDate());
    }

    @Test
    @DisplayName("resolveRangeStart(\"30d\") is 29 days before today in the business timezone")
    void testResolve30d_twentyNineDaysBack() {
        java.time.LocalDate expected = ZonedDateTime.now(ZoneId.of(KATHMANDU)).minusDays(29).toLocalDate();
        assertEquals(expected, clock.resolveRangeStart("30d").toLocalDate());
    }

    @Test
    @DisplayName("resolveRangeStart(\"90d\") is 89 days before today in the business timezone")
    void testResolve90d_eightyNineDaysBack() {
        java.time.LocalDate expected = ZonedDateTime.now(ZoneId.of(KATHMANDU)).minusDays(89).toLocalDate();
        assertEquals(expected, clock.resolveRangeStart("90d").toLocalDate());
    }

    @Test
    @DisplayName("resolveRangeStart(\"1y\") snaps to the 1st of the month 11 months ago")
    void testResolve1y_startOfMonthElevenMonthsBack() {
        LocalDateTime start = clock.resolveRangeStart("1y");
        ZonedDateTime expected = ZonedDateTime.now(ZoneId.of(KATHMANDU)).minusMonths(11);
        assertEquals(1, start.getDayOfMonth(), "1y must snap to the start of the month");
        assertEquals(expected.getMonth(), start.getMonth());
        assertEquals(expected.getYear(), start.getYear());
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
        assertEquals(0, start.getSecond());
        assertEquals(0, start.getNano());
    }

    @Test
    @DisplayName("null timeRange defaults to the 30d window")
    void testResolveNull_defaultsTo30d() {
        assertEquals(clock.resolveRangeStart("30d"), clock.resolveRangeStart(null));
    }

    @Test
    @DisplayName("Unknown tokens fall back to the 30d window")
    void testResolveUnknown_defaultsTo30d() {
        LocalDateTime expected = clock.resolveRangeStart("30d");
        assertEquals(expected, clock.resolveRangeStart("abc"));
        assertEquals(expected, clock.resolveRangeStart("999z"));
        assertEquals(expected, clock.resolveRangeStart(""));
    }

    @Test
    @DisplayName("Token matching is case-insensitive: \"7D\" == \"7d\"")
    void testResolve_caseInsensitive() {
        assertEquals(clock.resolveRangeStart("7d"), clock.resolveRangeStart("7D"));
        assertEquals(clock.resolveRangeStart("1y"), clock.resolveRangeStart("1Y"));
    }

    @Test
    @DisplayName("Token matching tolerates surrounding whitespace: \" 7d \" == \"7d\"")
    void testResolve_whitespaceTolerant() {
        assertEquals(clock.resolveRangeStart("7d"), clock.resolveRangeStart(" 7d "));
        assertEquals(clock.resolveRangeStart("30d"), clock.resolveRangeStart("\t30d\n"));
    }

    @Test
    @DisplayName("Deterministic: two consecutive calls return the exact same boundary")
    void testResolve_isDeterministic() {
        LocalDateTime first = clock.resolveRangeStart("30d");
        LocalDateTime second = clock.resolveRangeStart("30d");
        assertEquals(first, second, "boundaries must not slide by sub-second amounts");
        assertEquals(clock.resolveRangeStart("7d"), clock.resolveRangeStart("7d"));
        assertEquals(clock.resolveRangeStart("1y"), clock.resolveRangeStart("1y"));
    }

    @Test
    @DisplayName("Longer ranges start strictly earlier: 1y < 90d < 30d < 7d")
    void testResolve_rangeOrdering() {
        LocalDateTime oneYear = clock.resolveRangeStart("1y");
        LocalDateTime ninety = clock.resolveRangeStart("90d");
        LocalDateTime thirty = clock.resolveRangeStart("30d");
        LocalDateTime seven = clock.resolveRangeStart("7d");
        assertTrue(ninety.isBefore(thirty), "90d start must be before 30d start");
        assertTrue(thirty.isBefore(seven), "30d start must be before 7d start");
        assertTrue(oneYear.isBefore(ninety), "1y start must be before 90d start");
    }

    @Test
    @DisplayName("Invalid timezone string is rejected at construction time")
    void testInvalidTimezone_throws() {
        assertThrows(DateTimeException.class, () -> new ReportingClock("Not/AZone"));
        assertThrows(DateTimeException.class, () -> new ReportingClock("this is not a zone"));
    }

    @Test
    @DisplayName("now() and nowZoned() agree and are expressed in the business timezone")
    void testNow_usesBusinessZone() {
        ZonedDateTime zoned = clock.nowZoned();
        assertEquals(ZoneId.of(KATHMANDU), zoned.getZone());
        assertEquals(businessToday(), clock.now().toLocalDate());
    }
}
