package com.tripnext.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.expense.BalanceResponse;
import com.tripnext.demo.dto.expense.CreateExpenseRequest;
import com.tripnext.demo.dto.expense.CreateSettlementRequest;
import com.tripnext.demo.dto.expense.ExpenseResponse;
import com.tripnext.demo.dto.expense.SettlementResponse;
import com.tripnext.demo.dto.expense.SettlementSuggestionResponse;
import com.tripnext.demo.dto.expense.UpdateExpenseRequest;
import com.tripnext.demo.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;
@GetMapping
public ResponseEntity<Page<ExpenseResponse>>
getExpenses(
        @PathVariable UUID tripId,
        Pageable pageable
) {
    return ResponseEntity.ok(
            expenseService.getExpenses(
                    tripId,
                    pageable
            )
    );
}
@PostMapping
public ResponseEntity<ExpenseResponse> createExpense(
        @PathVariable UUID tripId,
        @RequestBody CreateExpenseRequest request,
        @RequestHeader("Idempotency-Key")
        String idempotencyKey
) {

    return ResponseEntity.status(
            HttpStatus.CREATED
    ).body(
            expenseService.createExpense(
                    tripId,
                    request,
                    idempotencyKey
            )
    );
}
    @PatchMapping("/{expenseId}")
public ResponseEntity<ExpenseResponse> updateExpense(
        @PathVariable UUID expenseId,
        @RequestBody UpdateExpenseRequest request
) {

    return ResponseEntity.ok(
            expenseService.updateExpense(
                    expenseId,
                    request
            )
    );
}
   @DeleteMapping("/{expenseId}")
public ResponseEntity<Void> deleteExpense(
        @PathVariable UUID expenseId
) {

    expenseService.deleteExpense(expenseId);

    return ResponseEntity.noContent().build();
}
@GetMapping("/balances")
public ResponseEntity<List<BalanceResponse>>
getBalances(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            expenseService.getBalances(
                    tripId
            )
    );
}
@PostMapping("/settlements")
public ResponseEntity<SettlementResponse>
createSettlement(
        @PathVariable UUID tripId,
        @RequestBody CreateSettlementRequest request
) {

    SettlementResponse response =
            expenseService.createSettlement(
                    tripId,
                    request
            );

    return ResponseEntity.status(201)
            .body(response);
}
@GetMapping("/settlements")
public ResponseEntity<List<SettlementResponse>>
getSettlements(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            expenseService.getSettlements(
                    tripId
            )
    );
}
@GetMapping("/settlements/suggestions")
public ResponseEntity<
        List<SettlementSuggestionResponse>>
getSettlementSuggestions(
        @PathVariable UUID tripId
) {

    return ResponseEntity.ok(
            expenseService
                    .getSettlementSuggestions(
                            tripId
                    )
    );
}
   
}
