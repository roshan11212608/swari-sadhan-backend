package swari.sewa.common.util;

/**
 * Minimal HTML escaping for values interpolated into server-rendered HTML.
 *
 * <p>Used by the invoice renderer. Invoice fields such as shop name, owner name,
 * address and plan name are user-controlled and persisted, so interpolating them
 * raw into the invoice template is a stored-XSS vector — and one that fires in a
 * Super Admin's browser, since Super Admins can view any shop's invoice.
 *
 * <p>Escapes the five characters that matter for HTML text and quoted attribute
 * contexts. Not intended for unquoted attributes, URLs, CSS or JavaScript
 * contexts; do not use it for those.
 */
public final class HtmlEscape {

    private HtmlEscape() {
    }

    /**
     * Escape a value for inclusion in HTML text or a quoted attribute.
     *
     * @param value raw value, may be null
     * @return escaped value, or an empty string when {@code value} is null
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Escape a value, substituting a placeholder when it is null or blank.
     * Keeps the invoice layout stable instead of rendering empty cells.
     */
    public static String escapeOr(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return escape(fallback);
        }
        return escape(value);
    }
}
