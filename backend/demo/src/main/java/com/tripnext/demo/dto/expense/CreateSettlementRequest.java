

package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSettlementRequest(
        UUID paidToUserId,
        BigDecimal amount
) {}