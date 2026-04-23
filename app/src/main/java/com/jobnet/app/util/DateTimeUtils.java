package com.jobnet.app.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class DateTimeUtils {

    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private static final DateTimeFormatter LOCAL_DT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter(Locale.ENGLISH);

    private DateTimeUtils() {
    }

    public static String formatDateTime(String preferred, String fallback) {
        String first = formatDateTime(preferred);
        if (!first.isBlank()) {
            return first;
        }
        return formatDateTime(fallback);
    }

    public static String formatDateTime(String raw) {
        LocalDateTime dt = parse(raw);
        if (dt == null) {
            return "";
        }
        return dt.format(OUTPUT);
    }

    public static String formatRelativeDateTime(String preferred, String fallback) {
        LocalDateTime dt = firstNonNull(parse(preferred), parse(fallback));
        if (dt != null) {
            return toRelative(dt);
        }

        String absolute = formatDateTime(preferred, fallback);
        if (!absolute.isBlank()) {
            return absolute;
        }

        String raw = normalize(preferred);
        if (!raw.isEmpty()) {
            return raw;
        }
        return normalize(fallback);
    }

    public static String formatRelativeForPosted(String postDateRaw, String dateTimeRaw, String updatedAtRaw) {
        LocalDateTime dt = firstNonNull(parse(dateTimeRaw), parse(updatedAtRaw));
        if (dt != null) {
            return toRelative(dt);
        }

        String normalized = normalize(postDateRaw);
        if (normalized.endsWith("D")) {
            String digits = normalized.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                int days = parseIntSafe(digits, -1);
                if (days == 0) {
                    return "Just now";
                }
                if (days > 0) {
                    return days + "d ago";
                }
            }
        }

        if (!normalize(postDateRaw).isEmpty()) {
            return postDateRaw.trim();
        }
        return "Recently posted";
    }

    public static String toRelative(LocalDateTime dt) {
        Duration duration = Duration.between(dt, LocalDateTime.now());
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds < 60) {
            return "Just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = hours / 24;
        return days + "d ago";
    }

    private static LocalDateTime parse(String raw) {
        String value = normalize(raw);
        if (value.isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(value, LOCAL_DT);
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static LocalDateTime firstNonNull(LocalDateTime a, LocalDateTime b) {
        return a != null ? a : b;
    }
}
