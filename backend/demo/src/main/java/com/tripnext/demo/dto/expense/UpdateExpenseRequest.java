package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tripnext.demo.model.help.ExpenseCategory;

/**
 * participantIds / splits are optional. If neither is sent and the amount
 * changes, the new amount is split equally among the current participants,
 * so splits always add up to the total.
 */
public record UpdateExpenseRequest(
        String title,
        String description,
        ExpenseCategory category,
        BigDecimal amount,
        List<UUID> participantIds,
        List<ExpenseSplitRequest> splits
) {}
