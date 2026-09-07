package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tripnext.demo.model.help.ExpenseCategory;

public record CreateExpenseRequest(
        String title,
        String description,
        ExpenseCategory category,
        BigDecimal amount,
        List<UUID> participantIds
) {
}