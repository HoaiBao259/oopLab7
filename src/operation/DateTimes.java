package operation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Formats and validates timestamps in the format shared by user and order records. */
final class DateTimes {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-uuuu_HH:mm:ss", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private DateTimes() {}

    /** Returns the current local time in the record format. */
    static String now() {
        return format(LocalDateTime.now());
    }

    /** Formats a local date and time for storage. */
    static String format(LocalDateTime dateTime) {
        return FORMAT.format(dateTime);
    }

    /** Returns a parsed timestamp, or null if the value is absent or invalid. */
    static LocalDateTime parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
