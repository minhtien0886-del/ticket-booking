package com.club.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all domain entities that participate in CSV persistence.
 * Defines the contract for CSV serialisation/deserialisation ({@code toCsvLine} /
 * {@code fromCsvLine}) and provides shared utility methods that eliminate code
 * duplication across the 16 entity types.
 *
 * <p>Every entity backed by a CSV data file must extend this class (or implement
 * the CSV contract via its static factory). The instance method {@link #toCsvLine()}
 * serialises the entity, while the complementary static {@code fromCsvLine(String)}
 * method on each subclass reconstructs it.</p>
 *
 * <h3>Design Rationale</h3>
 * <ul>
 *   <li><b>DRY:</b> The {@link #safe(String)} and {@link #parseCsvLine(String)}
 *       helpers were duplicated verbatim across 16 entities. Centralising them
 *       here ensures a single, tested implementation.</li>
 *   <li><b>Contract enforcement:</b> Making {@code toCsvLine()} abstract forces
 *       every new entity to implement CSV serialisation before it can compile.</li>
 *   <li><b>Type safety:</b> {@code GenericCsvRepository} can now accept
 *       {@code BaseEntity} as an upper bound for compile-time safety.</li>
 * </ul>
 *
 * <h3>CSV Quoting Rules</h3>
 * <p>Fields containing commas are wrapped in double quotes. Null values are
 * serialised as empty strings. This matches RFC 4180 minimal quoting.</p>
 *
 * @author FCM-ERP Architecture Team
 * @version 1.0
 * @since Java 8
 * @see com.club.repository.GenericCsvRepository
 */
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // ABSTRACT CONTRACT — every entity must implement CSV serialisation
    // =========================================================================

    /**
     * Serialises this entity into a single CSV row string.
     *
     * <p>The returned string must have fields in the exact same order as the
     * CSV header defined in the corresponding repository. Fields are separated
     * by commas, and values containing commas are quoted.</p>
     *
     * @return a comma-separated value string representing this entity
     */
    public abstract String toCsvLine();

    /**
     * Returns the unique identifier of this entity.
     *
     * <p>Used by {@code GenericCsvRepository} as the cache key. The returned
     * value must be stable (not change between calls) and non-null for
     * entities that are stored in the repository.</p>
     *
     * @return the entity's unique string identifier
     */
    public abstract String getEntityId();

    // =========================================================================
    // SHARED UTILITY — CSV field escaping
    // =========================================================================

    /**
     * Escapes a string value for safe inclusion in a CSV row.
     *
     * <ul>
     *   <li>{@code null} → empty string {@code ""}</li>
     *   <li>Value containing a comma → wrapped in double quotes</li>
     *   <li>All other values → returned as-is</li>
     * </ul>
     *
     * <p>This method is intentionally {@code protected static} so that both
     * instance methods ({@code toCsvLine()}) and static factory methods
     * ({@code fromCsvLine()}) in subclasses can use it.</p>
     *
     * @param s the string to escape; may be null
     * @return a CSV-safe representation of the value
     */
    protected static String safe(String s) {
        if (s == null) return "";
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    // =========================================================================
    // SHARED UTILITY — CSV line parsing
    // =========================================================================

    /**
     * Parses a raw CSV line into an array of field values, correctly handling
     * quoted fields that may contain commas.
     *
     * <p><b>Algorithm:</b> Single-pass character scan with a boolean flag
     * tracking whether the scanner is inside a quoted region. Fields are
     * delimited by commas outside of quotes.</p>
     *
     * <h4>Time Complexity</h4>
     * <p>O(L) where L is the length of the CSV line in characters.</p>
     *
     * <h4>Examples</h4>
     * <pre>
     * parseCsvLine("a,b,c")           → ["a", "b", "c"]
     * parseCsvLine("a,\"b,c\",d")     → ["a", "b,c", "d"]
     * parseCsvLine("")                → [""]
     * parseCsvLine(null)              → String[0]  (empty array)
     * </pre>
     *
     * @param csv the raw CSV line to parse; may be null
     * @return an array of parsed field values (never null)
     */
    protected static String[] parseCsvLine(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new String[0];
        }
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : csv.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Safely retrieves a value from a parsed CSV parts array by index.
     * Returns the default value if the index is out of bounds or the
     * field is empty.
     *
     * @param parts        the parsed CSV fields array
     * @param index        the zero-based field index
     * @param defaultValue the fallback value if the field is missing or empty
     * @return the field value, or the default
     */
    protected static String getField(String[] parts, int index, String defaultValue) {
        if (parts == null || index < 0 || index >= parts.length) {
            return defaultValue;
        }
        String value = parts[index];
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * Safely parses an integer from a CSV parts array.
     *
     * @param parts        the parsed CSV fields array
     * @param index        the zero-based field index
     * @param defaultValue the fallback if parsing fails
     * @return the parsed int, or the default
     */
    protected static int getIntField(String[] parts, int index, int defaultValue) {
        try {
            String val = getField(parts, index, null);
            return val != null ? Integer.parseInt(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double from a CSV parts array.
     *
     * @param parts        the parsed CSV fields array
     * @param index        the zero-based field index
     * @param defaultValue the fallback if parsing fails
     * @return the parsed double, or the default
     */
    protected static double getDoubleField(String[] parts, int index, double defaultValue) {
        try {
            String val = getField(parts, index, null);
            return val != null ? Double.parseDouble(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a boolean from a CSV parts array.
     *
     * @param parts        the parsed CSV fields array
     * @param index        the zero-based field index
     * @param defaultValue the fallback if parsing fails
     * @return the parsed boolean, or the default
     */
    protected static boolean getBooleanField(String[] parts, int index, boolean defaultValue) {
        String val = getField(parts, index, null);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }
}
