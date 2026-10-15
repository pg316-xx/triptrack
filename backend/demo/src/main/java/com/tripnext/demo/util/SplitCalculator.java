package com.tripnext.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Money splitting with the invariant sum(splits) == total (SRS FR-EXP-003).
 * Amounts are in currency units with 2 decimal places.
 */
public final class SplitCalculator {

    private SplitCalculator() {
    }

    /**
     * Splits {@code total} into {@code parts} shares that differ by at most
     * one cent. Leftover cents go to the first shares, e.g. 100.00 / 3 =
     * [33.34, 33.33, 33.33].
     */
    public static List<BigDecimal> equalSplit(BigDecimal total, int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("parts must be positive");
        }
        long cents = toCents(total);
        long base = cents / parts;
        long remainder = cents % parts;

        List<BigDecimal> shares = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            long share = base + (i < remainder ? 1 : 0);
            shares.add(BigDecimal.valueOf(share, 2));
        }
        return shares;
    }

    /** Sum of the given shares (null entries are not allowed). */
    public static BigDecimal sum(List<BigDecimal> shares) {
        return shares.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    /** True if the amount has at most 2 decimal places. */
    public static boolean hasValidScale(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 2;
    }

    private static long toCents(BigDecimal amount) {
        if (!hasValidScale(amount)) {
            throw new IllegalArgumentException("At most 2 decimal places allowed");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }
}
