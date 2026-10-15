package com.tripnext.demo.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class LexoRankTest {

    @Test
    void firstRankIsNotEmpty() {
        String r = LexoRank.between(null, null);
        assertFalse(r.isEmpty());
    }

    @Test
    void appendingKeepsIncreasing() {
        String prev = LexoRank.between(null, null);
        for (int i = 0; i < 500; i++) {
            String next = LexoRank.between(prev, null);
            assertTrue(next.compareTo(prev) > 0, next + " > " + prev);
            prev = next;
        }
    }

    @Test
    void prependingKeepsDecreasing() {
        String prev = LexoRank.between(null, null);
        for (int i = 0; i < 500; i++) {
            String next = LexoRank.between(null, prev);
            assertTrue(next.compareTo(prev) < 0, next + " < " + prev);
            prev = next;
        }
    }

    @Test
    void repeatedlySplittingTheSameGapAlwaysFits() {
        String lo = LexoRank.between(null, null);
        String hi = LexoRank.between(lo, null);
        for (int i = 0; i < 200; i++) {
            String mid = LexoRank.between(lo, hi);
            assertTrue(lo.compareTo(mid) < 0 && mid.compareTo(hi) < 0,
                    lo + " < " + mid + " < " + hi);
            if (i % 2 == 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
    }

    @Test
    void randomInsertsStayOrdered() {
        Random random = new Random(42);
        List<String> ranks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int pos = random.nextInt(ranks.size() + 1);
            String before = pos == 0 ? null : ranks.get(pos - 1);
            String after = pos == ranks.size() ? null : ranks.get(pos);
            String r = LexoRank.between(before, after);
            assertFalse(r.endsWith("0"), "rank must not end in 0: " + r);
            ranks.add(pos, r);
        }
        for (int i = 1; i < ranks.size(); i++) {
            assertTrue(ranks.get(i - 1).compareTo(ranks.get(i)) < 0);
        }
    }

    @Test
    void migratedIntegerRanksCanBeSplit() {
        // V4 converts old integer order_index values to e.g. "00000000011"
        String a = "00000000011";
        String b = "00000000021";
        String mid = LexoRank.between(a, b);
        assertTrue(a.compareTo(mid) < 0 && mid.compareTo(b) < 0);
        assertTrue(LexoRank.between(b, null).compareTo(b) > 0);
        assertTrue(LexoRank.between(null, a).compareTo(a) < 0);
    }

    @Test
    void rejectsOutOfOrderNeighbours() {
        assertThrows(IllegalArgumentException.class,
                () -> LexoRank.between("b", "a"));
        assertThrows(IllegalArgumentException.class,
                () -> LexoRank.between("a", "a"));
    }
}
