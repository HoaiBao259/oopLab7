package operation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the flat JSON objects used in line-delimited storage. */
final class FlatJson {
    private static final String STRING =
            "\"((?:[^\"\\\\\\x00-\\x1f]|\\\\[\"\\\\/bfnrt]|\\\\u[0-9a-fA-F]{4})*+)\"";
    private static final String SCALAR =
            "(-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?|true|false|null)";
    private static final Pattern FIELD =
            Pattern.compile("\\s*" + STRING + "\\s*:\\s*(?:" + STRING + "|" + SCALAR + ")\\s*([},])");

    private FlatJson() {}

    /**
     * Reads one flat object and normalizes unquoted numeric values to strings.
     *
     * @param line the input record
     * @return parsed fields, or an empty map for a malformed record
     */
    static Map<String, String> parse(String line) {
        if (line == null) {
            return Map.of();
        }
        String record = line.strip();
        if (record.startsWith("\uFEFF")) {
            record = record.substring(1).stripLeading();
        }
        if (!record.startsWith("{") || !record.endsWith("}")) {
            return Map.of();
        }
        Map<String, String> data = new LinkedHashMap<>();
        Matcher field = FIELD.matcher(record);
        int offset = 1;
        while (field.region(offset, record.length()).lookingAt()) {
            String key = decode(field.group(1));
            if (data.containsKey(key)) {
                return Map.of();
            }
            String scalar = field.group(3);
            String value = field.group(2) != null ? decode(field.group(2)) : scalar;
            data.put(key, "null".equals(scalar) ? null : value);
            offset = field.end();
            if ("}".equals(field.group(4))) {
                return offset == record.length() ? data : Map.of();
            }
        }
        return Map.of();
    }

    private static String decode(String value) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character != '\\') {
                decoded.append(character);
                continue;
            }
            char escape = value.charAt(++i);
            switch (escape) {
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'u' -> {
                    decoded.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> decoded.append(escape);
            }
        }
        return decoded.toString();
    }
}
