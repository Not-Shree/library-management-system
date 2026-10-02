package com.college.library.util;

/** Small string helpers used by services. */
public final class TextUtils {

    private TextUtils() {
    }

    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** Trims and turns blank strings into null. */
    public static String clean(String s) {
        return hasText(s) ? s.trim() : null;
    }

    /** Builds a lower-case "%text%" pattern for LIKE searches, escaping LIKE wildcards. */
    public static String likePattern(String s) {
        String escaped = s.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
