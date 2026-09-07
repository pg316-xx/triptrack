package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementSuggestionResponse(
        UUID fromUserId,
        String fromUserName,
        UUID toUserId,
        String toUserName,
        BigDecimal amount
) {}