package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

/** One person's share of an expense (custom split). */
public record ExpenseSplitRequest(
        UUID userId,
        BigDecimal amount
) {}
