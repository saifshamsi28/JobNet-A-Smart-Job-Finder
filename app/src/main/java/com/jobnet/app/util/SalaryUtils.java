package com.jobnet.app.util;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SalaryUtils {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final double USD_TO_INR = 83.0;

    private SalaryUtils() {
    }

    public static String normalizeDisplay(String rawSalary) {
        String raw = safe(rawSalary);
        if (raw.isEmpty()) {
            return "Salary not disclosed";
        }

        if (containsAny(raw, "₹", "INR", "LPA", "LAC", "LAKH")) {
            return normalizeIndianSalary(raw);
        }

        if (raw.contains("$")) {
            return convertUsdToInrLpa(raw);
        }

        if (looksLikePlainNumericSalary(raw)) {
            return normalizeNumericSalary(raw);
        }

        return raw;
    }

    public static int approximateLpa(String rawSalary) {
        String raw = safe(rawSalary).toUpperCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return 0;
        }

        if (raw.contains("$")) {
            double first = extractFirstNumber(raw);
            if (first <= 0) {
                return 0;
            }
            if (raw.contains("K")) {
                first *= 1_000;
            } else if (raw.contains("M")) {
                first *= 1_000_000;
            }
            double lpa = (first * USD_TO_INR) / 100_000d;
            return (int) Math.round(lpa);
        }

        if (containsAny(raw, "LPA", "LAC", "LAKH")) {
            return (int) Math.round(extractFirstNumber(raw));
        }

        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return 0;
        }
        double first = parseDoubleSafe(matcher.group(1));
        if (first <= 0) {
            return 0;
        }

        if (first > 1000) {
            return (int) Math.round(first / 100_000d);
        }
        return (int) Math.round(first);
    }

    private static String normalizeIndianSalary(String raw) {
        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        double first = 0;
        double second = 0;
        if (matcher.find()) {
            first = parseDoubleSafe(matcher.group(1));
        }
        if (matcher.find()) {
            second = parseDoubleSafe(matcher.group(1));
        }

        if (first <= 0) {
            return raw;
        }

        String firstText = trimTrailingZeros(first);
        if (second > 0) {
            return "₹" + firstText + "-" + trimTrailingZeros(second) + " LPA";
        }
        return "₹" + firstText + " LPA";
    }

    private static String convertUsdToInrLpa(String raw) {
        Matcher matcher = NUMBER_PATTERN.matcher(raw.toUpperCase(Locale.ROOT));
        double first = 0;
        double second = 0;

        if (matcher.find()) {
            first = parseDoubleSafe(matcher.group(1));
        }
        if (matcher.find()) {
            second = parseDoubleSafe(matcher.group(1));
        }
        if (first <= 0) {
            return raw;
        }

        double multiplier = 1;
        String upper = raw.toUpperCase(Locale.ROOT);
        if (upper.contains("K")) {
            multiplier = 1_000;
        } else if (upper.contains("M")) {
            multiplier = 1_000_000;
        }

        double firstLpa = ((first * multiplier) * USD_TO_INR) / 100_000d;
        if (second > 0) {
            double secondLpa = ((second * multiplier) * USD_TO_INR) / 100_000d;
            return "₹" + trimTrailingZeros(firstLpa) + "-" + trimTrailingZeros(secondLpa) + " LPA";
        }
        return "₹" + trimTrailingZeros(firstLpa) + " LPA";
    }

    private static String normalizeNumericSalary(String raw) {
        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        double first = 0;
        double second = 0;
        if (matcher.find()) {
            first = parseDoubleSafe(matcher.group(1));
        }
        if (matcher.find()) {
            second = parseDoubleSafe(matcher.group(1));
        }

        if (first <= 0) {
            return raw;
        }

        if (first > 1000) {
            first /= 100_000d;
        }
        if (second > 1000) {
            second /= 100_000d;
        }

        if (second > 0) {
            return "₹" + trimTrailingZeros(first) + "-" + trimTrailingZeros(second) + " LPA";
        }
        return "₹" + trimTrailingZeros(first) + " LPA";
    }

    private static boolean looksLikePlainNumericSalary(String raw) {
        String upper = raw.toUpperCase(Locale.ROOT);
        return upper.matches(".*\\d.*") && !containsAny(upper, "USD", "EUR", "GBP", "PER HOUR", "/HR");
    }

    private static double extractFirstNumber(String raw) {
        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return 0;
        }
        return parseDoubleSafe(matcher.group(1));
    }

    private static boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token != null && upper.contains(token.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String trimTrailingZeros(double value) {
        DecimalFormat format = new DecimalFormat("0.##");
        return format.format(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
