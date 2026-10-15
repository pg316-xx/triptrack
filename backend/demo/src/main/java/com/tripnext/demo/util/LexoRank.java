package com.tripnext.demo.util;

/**
 * Fractional lexicographic ranks for ordering activities within a day.
 *
 * A rank is a base-36 string ([0-9a-z]) compared with plain String ordering
 * (the DB column uses COLLATE "C" to match). Moving an item only rewrites
 * that item's rank: {@link #between} always finds a string strictly between
 * its neighbours, so no other rows need re-indexing.
 *
 * Ranks never end in '0', which guarantees there is always room between two
 * adjacent ranks.
 */
public final class LexoRank {

    private static final String DIGITS =
            "0123456789abcdefghijklmnopqrstuvwxyz";

    private LexoRank() {
    }

    /**
     * Returns a rank strictly between {@code before} and {@code after}.
     *
     * @param before rank of the previous item, or null to insert at the start
     * @param after  rank of the next item, or null to insert at the end
     */
    public static String between(String before, String after) {
        String a = before == null ? "" : before;

        if (after != null && a.compareTo(after) >= 0) {
            throw new IllegalArgumentException(
                    "Rank '" + a + "' must sort before '" + after + "'"
            );
        }
        return midpoint(a, after);
    }

    private static String midpoint(String a, String b) {
        if (b != null) {
            // Copy the shared prefix, then find a midpoint in what's left.
            int n = 0;
            while (n < b.length() && charAt(a, n) == b.charAt(n)) {
                n++;
            }
            if (n > 0) {
                return b.substring(0, n)
                        + midpoint(
                                a.length() > n ? a.substring(n) : "",
                                b.substring(n)
                        );
            }
        }

        int digitA = a.isEmpty() ? 0 : DIGITS.indexOf(a.charAt(0));
        int digitB = b != null ? DIGITS.indexOf(b.charAt(0)) : DIGITS.length();

        if (digitB - digitA > 1) {
            return String.valueOf(
                    DIGITS.charAt(Math.round(0.5f * (digitA + digitB)))
            );
        }

        // Adjacent first digits: keep a's digit and go one level deeper.
        if (b != null && b.length() > 1) {
            return b.substring(0, 1);
        }
        return DIGITS.charAt(digitA)
                + midpoint(a.length() > 1 ? a.substring(1) : "", null);
    }

    private static char charAt(String s, int i) {
        return i < s.length() ? s.charAt(i) : DIGITS.charAt(0);
    }
}
