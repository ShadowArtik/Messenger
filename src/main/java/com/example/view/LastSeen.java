package com.example.view;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class LastSeen {

    // =================== Formatting ===================

    private LastSeen() {
    }

    private static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static String format(long epochMillis) {
        ZonedDateTime seen = Instant.ofEpochMilli(epochMillis).atZone(ZONE);
        ZonedDateTime now = ZonedDateTime.now(ZONE);

        long minutes = Duration.between(seen, now).toMinutes();

        if (minutes < 1) {
            return "last seen just now";
        }

        if (minutes < 60) {
            return "last seen " + minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        if (seen.toLocalDate().equals(now.toLocalDate())) {
            return "last seen today at " + seen.format(TIME);
        }

        if (seen.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "last seen yesterday at " + seen.format(TIME);
        }

        return "last seen " + seen.format(DATE);
    }
}
