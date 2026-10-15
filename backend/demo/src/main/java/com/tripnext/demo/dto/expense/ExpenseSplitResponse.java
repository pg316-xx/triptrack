package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSplitResponse(
        UUID userId,
        String userName,
        BigDecimal amount
) {}
