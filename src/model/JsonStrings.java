package model;

/** Quotes string fields in the line-delimited JSON representations of models. */
public final class JsonStrings {
    private static final String HEX_DIGITS = "0123456789abcdef";

    private JsonStrings() {}

    /**
     * Encodes a value as a JSON string literal.
     *
     * @param value value to encode
     * @return a quoted and escaped value, or the JSON null literal
     */
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"' || character == '\\') {
                escaped.append('\\').append(character);
            } else if (character < ' ' || Character.isSurrogate(character)) {
                // The password encoding can place filler between the halves of a surrogate pair.
                escaped.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    escaped.append(HEX_DIGITS.charAt(character >> shift & 0xf));
                }
            } else {
                escaped.append(character);
            }
        }
        return escaped.append('"').toString();
    }
}
