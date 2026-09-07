package com.tripnext.demo.dto.expense;

import java.math.BigDecimal;

import com.tripnext.demo.model.help.ExpenseCategory;


public record UpdateExpenseRequest(
        String title,
        String description,
        ExpenseCategory category,
        BigDecimal amount
) {}

