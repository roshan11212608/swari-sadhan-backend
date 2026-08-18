package swari.sewa.module.analytics.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class DateFilterUtil {

    public static DateRange getDateRange(String filter) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // Support custom month filter: "YYYY-MM" (e.g. "2025-07")
        if (filter != null && filter.matches("^\\d{4}-\\d{2}$")) {
            try {
                YearMonth ym = YearMonth.parse(filter);
                LocalDate monthStart = ym.atDay(1);
                LocalDate monthEnd = ym.atEndOfMonth();
                return new DateRange(monthStart.atStartOfDay(), monthEnd.atTime(LocalTime.MAX));
            } catch (Exception e) {
                // fall through to default
            }
        }

        switch (filter.toLowerCase()) {
            case "today":
                return new DateRange(today.atStartOfDay(), now);
            case "yesterday":
                LocalDate yesterday = today.minusDays(1);
                return new DateRange(yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX));
            case "thisweek":
                WeekFields weekFields = WeekFields.of(Locale.getDefault());
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(weekFields.getFirstDayOfWeek()));
                return new DateRange(startOfWeek.atStartOfDay(), now);
            case "thismonth":
                LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
                return new DateRange(startOfMonth.atStartOfDay(), now);
            case "lastmonth":
                LocalDate lastMonth = today.minusMonths(1);
                LocalDate startOfLastMonth = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate endOfLastMonth = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
                return new DateRange(startOfLastMonth.atStartOfDay(), endOfLastMonth.atTime(LocalTime.MAX));
            case "thisyear":
                LocalDate startOfYear = today.with(TemporalAdjusters.firstDayOfYear());
                return new DateRange(startOfYear.atStartOfDay(), now);
            default:
                return new DateRange(today.atStartOfDay(), now);
        }
    }

    public static DateRange getCustomDateRange(LocalDate from, LocalDate to) {
        return new DateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }

    public static String getPeriodLabel(LocalDateTime dateTime, String filter) {
        if (filter.equalsIgnoreCase("thisyear")) {
            return String.valueOf(dateTime.getYear());
        } else {
            return dateTime.getMonth().name().substring(0, 3);
        }
    }

    public static class DateRange {
        private final LocalDateTime from;
        private final LocalDateTime to;

        public DateRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }

        public LocalDateTime getFrom() {
            return from;
        }

        public LocalDateTime getTo() {
            return to;
        }
    }
}
