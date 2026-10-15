package com.tripnext.demo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class SplitCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void leftoverCentsGoToTheFirstShares() {
        assertEquals(
                List.of(bd("33.34"), bd("33.33"), bd("33.33")),
                SplitCalculator.equalSplit(bd("100"), 3)
        );
        assertEquals(
                List.of(bd("0.01"), bd("0.01"), bd("0.00")),
                SplitCalculator.equalSplit(bd("0.02"), 3)
        );
    }

    @Test
    void evenAmountsSplitEvenly() {
        assertEquals(
                List.of(bd("25.00"), bd("25.00"), bd("25.00"), bd("25.00")),
                SplitCalculator.equalSplit(bd("100.00"), 4)
        );
    }

    @Test
    void equalSplitAlwaysSumsToTotal() {
        Random random = new Random(7);
        for (int i = 0; i < 5000; i++) {
            BigDecimal total = BigDecimal.valueOf(1 + random.nextInt(10_000_000), 2);
            int parts = 1 + random.nextInt(25);
            List<BigDecimal> shares = SplitCalculator.equalSplit(total, parts);

            assertEquals(parts, shares.size());
            assertEquals(0, SplitCalculator.sum(shares).compareTo(total),
                    total + " / " + parts + " -> " + shares);

            BigDecimal max = shares.stream().max(BigDecimal::compareTo).orElseThrow();
            BigDecimal min = shares.stream().min(BigDecimal::compareTo).orElseThrow();
            assertTrue(max.subtract(min).compareTo(bd("0.01")) <= 0, "shares differ by at most a cent");
        }
    }

    @Test
    void sumUsesExactDecimalArithmetic() {
        // 0.1 + 0.2 must be exactly 0.30, not 0.30000000000000004
        assertEquals(bd("0.30"), SplitCalculator.sum(List.of(bd("0.1"), bd("0.2"))));
    }

    @Test
    void scaleCheck() {
        assertTrue(SplitCalculator.hasValidScale(bd("10")));
        assertTrue(SplitCalculator.hasValidScale(bd("10.5")));
        assertTrue(SplitCalculator.hasValidScale(bd("10.50")));
        assertTrue(SplitCalculator.hasValidScale(bd("10.500")));
        assertFalse(SplitCalculator.hasValidScale(bd("10.505")));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.equalSplit(bd("10"), 0));
        assertThrows(IllegalArgumentException.class, () -> SplitCalculator.equalSplit(bd("10.001"), 2));
    }
}
