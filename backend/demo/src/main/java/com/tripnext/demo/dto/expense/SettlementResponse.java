package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID paidByUserId,
        UUID paidToUserId,
        BigDecimal amount,
        OffsetDateTime createdAt
) {}