package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tripnext.demo.model.help.ExpenseCategory;

/**
 * Send exactly one of:
 *  - participantIds: split equally (leftover cents go to the first participants)
 *  - splits: custom amounts per person, which must add up exactly to amount
 */
public record CreateExpenseRequest(
        String title,
        String description,
        ExpenseCategory category,
        BigDecimal amount,
        List<UUID> participantIds,
        List<ExpenseSplitRequest> splits
) {
}
