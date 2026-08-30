package swari.sewa.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HtmlEscape}.
 *
 * Business/security rule:
 *   Invoice fields (shop name, owner name, address, plan name) are user-controlled
 *   and persisted, then interpolated into a server-rendered HTML invoice that a
 *   Super Admin may open. Any of the five HTML-significant characters
 *   ({@code & < > " '}) must therefore be turned into its entity form so a stored
 *   payload can never break out of text or quoted-attribute context.
 *
 *   - escape(null)  -> "" (keeps the template rendering instead of "null")
 *   - escapeOr(value, fallback) -> escaped fallback when value is null or blank
 *   - '&' must be escaped as part of the same single pass so already-produced
 *     entities are never double-escaped.
 */
class HtmlEscapeTest {

    @Test
    @DisplayName("escape(null) returns an empty string, never the literal \"null\"")
    void escape_null_returnsEmptyString() {
        assertEquals("", HtmlEscape.escape(null));
    }

    @Test
    @DisplayName("Plain text with no HTML-significant characters passes through unchanged")
    void escape_plainText_unchanged() {
        assertEquals("Acme Traders Pvt Ltd", HtmlEscape.escape("Acme Traders Pvt Ltd"));
        assertEquals("Kathmandu 44600", HtmlEscape.escape("Kathmandu 44600"));
    }

    @Test
    @DisplayName("<script>alert(1)</script> is fully escaped and leaves no raw '<'")
    void escape_scriptTag_fullyEscaped() {
        String result = HtmlEscape.escape("<script>alert(1)</script>");
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", result);
        assertFalse(result.contains("<"), "No raw '<' may survive escaping: " + result);
        assertFalse(result.contains(">"), "No raw '>' may survive escaping: " + result);
    }

    @Test
    @DisplayName("Ampersand is escaped to &amp;")
    void escape_ampersand() {
        assertEquals("Ram &amp; Sons", HtmlEscape.escape("Ram & Sons"));
    }

    @Test
    @DisplayName("Double quote becomes &quot; and single quote becomes &#39;")
    void escape_quotes() {
        assertEquals("&quot;", HtmlEscape.escape("\""));
        assertEquals("&#39;", HtmlEscape.escape("'"));
        assertEquals("say &quot;hi&quot; to O&#39;Brien",
                HtmlEscape.escape("say \"hi\" to O'Brien"));
    }

    @Test
    @DisplayName("Malicious shop name Acme<img src=x onerror=alert(1)> is neutralised")
    void escape_maliciousShopName_neutralised() {
        String result = HtmlEscape.escape("Acme<img src=x onerror=alert(1)>");
        assertFalse(result.contains("<"), "Result must contain no '<': " + result);
        assertFalse(result.contains(">"), "Result must contain no '>': " + result);
        assertEquals("Acme&lt;img src=x onerror=alert(1)&gt;", result);
    }

    @Test
    @DisplayName("Escaping applies to EVERY occurrence, not just the first")
    void escape_allOccurrences() {
        assertEquals("&lt;&lt;&lt;", HtmlEscape.escape("<<<"));
        assertEquals("a&amp;b&amp;c&amp;d", HtmlEscape.escape("a&b&c&d"));
        String result = HtmlEscape.escape("<b><i><u>");
        assertEquals("&lt;b&gt;&lt;i&gt;&lt;u&gt;", result);
        assertFalse(result.contains("<"));
    }

    @Test
    @DisplayName("escapeOr(null, \"\u2014\") returns the escaped fallback")
    void escapeOr_nullValue_returnsFallback() {
        assertEquals("\u2014", HtmlEscape.escapeOr(null, "\u2014"));
        // Fallback itself is escaped too, so an unsafe fallback cannot inject markup.
        assertEquals("&lt;n/a&gt;", HtmlEscape.escapeOr(null, "<n/a>"));
    }

    @Test
    @DisplayName("escapeOr(\"   \", \"N/A\") treats whitespace-only as absent and returns the fallback")
    void escapeOr_blankValue_returnsFallback() {
        assertEquals("N/A", HtmlEscape.escapeOr("   ", "N/A"));
        assertEquals("N/A", HtmlEscape.escapeOr("", "N/A"));
    }

    @Test
    @DisplayName("escapeOr(\"real\", \"fallback\") returns the real value")
    void escapeOr_presentValue_returnsValue() {
        assertEquals("real", HtmlEscape.escapeOr("real", "fallback"));
    }

    @Test
    @DisplayName("'&' escaping never double-escapes: '<' yields exactly &lt;, not &amp;lt;")
    void escape_noDoubleEscaping() {
        assertEquals("&lt;", HtmlEscape.escape("<"));
        assertFalse(HtmlEscape.escape("<").contains("&amp;"),
                "The '&' introduced by &lt; must not itself be escaped");

        // A raw ampersand followed by 'lt;' in the input is escaped once only.
        assertEquals("&amp;lt;", HtmlEscape.escape("&lt;"));

        // Escaping is not idempotent-safe by design: escaping twice double-escapes,
        // which proves each character is visited exactly once per call.
        assertEquals("&amp;lt;", HtmlEscape.escape(HtmlEscape.escape("<")));
    }
}
