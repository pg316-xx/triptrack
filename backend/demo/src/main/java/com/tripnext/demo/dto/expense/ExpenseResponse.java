package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.tripnext.demo.model.help.ExpenseCategory;

public record ExpenseResponse(
        UUID id,
        String title,
        String description,
        ExpenseCategory category,
        BigDecimal amount,
        UUID paidByUserId,
        OffsetDateTime createdAt,
        List<ExpenseSplitResponse> splits
) {
}
