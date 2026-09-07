package com.tripnext.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.expense.BalanceResponse;
import com.tripnext.demo.dto.expense.CreateExpenseRequest;
import com.tripnext.demo.dto.expense.CreateSettlementRequest;
import com.tripnext.demo.dto.expense.ExpenseResponse;
import com.tripnext.demo.dto.expense.SettlementResponse;
import com.tripnext.demo.dto.expense.SettlementSuggestionResponse;
import com.tripnext.demo.dto.expense.UpdateExpenseRequest;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Expense;
import com.tripnext.demo.model.ExpenseSplit;
import com.tripnext.demo.model.IdempotencyKey;
import com.tripnext.demo.model.Settlement;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;
import com.tripnext.demo.repo.ExpenseRepo;
import com.tripnext.demo.repo.ExpenseSplitRepo;
import com.tripnext.demo.repo.IdempotencyKeyRepo;
import com.tripnext.demo.repo.SettlementRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.UserRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final UserRepo userRepo;
    private final CurrentUserService currentUserService;
    private final SettlementRepo settlementRepo;
    private final IdempotencyKeyRepo idempotencyKeyRepo;
    @Transactional
public ExpenseResponse createExpense(
        UUID tripId,
        CreateExpenseRequest request,
        String idempotencyKey
) {
   Optional<IdempotencyKey> existingKey =
        idempotencyKeyRepo.findByKey(
                idempotencyKey
        );

if (existingKey.isPresent()) {

    Expense existingExpense =
            expenseRepo.findById(
                    existingKey.get()
                            .getResourceId()
            ).orElseThrow();

    return new ExpenseResponse(
            existingExpense.getId(),
            existingExpense.getTitle(),
            existingExpense.getDescription(),
            existingExpense.getCategory(),
            existingExpense.getAmount(),
            existingExpense.getPaidBy().getId(),
            existingExpense.getCreatedAt()
    );
}
    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    boolean isMember =
            tripMemberRepo.existsByTripAndUser(
                    trip,
                    currentUser
            );

    if (!isMember) {
        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    if (request.participantIds().isEmpty()) {
        throw new IllegalArgumentException(
                "At least one participant required"
        );
    }

    Set<UUID> uniqueParticipants =
            new HashSet<>(request.participantIds());

    if (uniqueParticipants.size()
            != request.participantIds().size()) {

        throw new IllegalArgumentException(
                "Duplicate participants are not allowed"
        );
    }
    if (request.amount() == null
        || request.amount().compareTo(BigDecimal.ZERO) <= 0) {

    throw new IllegalArgumentException(
            "Amount must be greater than zero"
    );
}
    if (request.title() == null
        || request.title().isBlank()) {

    throw new IllegalArgumentException(
            "Title is required"
    );
}

    Expense expense = new Expense(
            trip,
            currentUser,
            request.amount(),
            request.title(),
            request.description(),
            request.category()
    );

    Expense savedExpense =
            expenseRepo.save(expense);
    idempotencyKeyRepo.save(
        new IdempotencyKey(
                idempotencyKey,
                savedExpense.getId(),
                "EXPENSE"
        )
);
    BigDecimal splitAmount =
            request.amount().divide(
                    BigDecimal.valueOf(
                            request.participantIds().size()
                    ),
                    2,
                    RoundingMode.HALF_UP
            );

    for (UUID participantId :
            request.participantIds()) {

        User participant =
                userRepo.findById(participantId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        boolean participantIsMember =
                tripMemberRepo.existsByTripAndUser(
                        trip,
                        participant
                );

        if (!participantIsMember) {
            throw new IllegalArgumentException(
                    "Participant is not a trip member"
            );
        }

        ExpenseSplit split =
                new ExpenseSplit(
                        savedExpense,
                        participant,
                        splitAmount
                );

        expenseSplitRepo.save(split);
    }

    return new ExpenseResponse(
            savedExpense.getId(),
            savedExpense.getTitle(),
            savedExpense.getDescription(),
            savedExpense.getCategory(),
            savedExpense.getAmount(),
            savedExpense.getPaidBy().getId(),
            savedExpense.getCreatedAt()
    );
}
public Page<ExpenseResponse> getExpenses(
        UUID tripId,
        Pageable pageable
) {

    User currentUser = currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(
            trip,
            currentUser
    )) {

        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    return expenseRepo
        .findByTripOrderByCreatedAtDesc(
                trip,
                pageable
        )
        .map(expense -> new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getCreatedAt()
        ));
}
@Transactional
public ExpenseResponse updateExpense(
        UUID expenseId,
        UpdateExpenseRequest request
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Expense expense = expenseRepo.findById(expenseId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Expense not found"
                    )
            );

    if (!expense.getPaidBy()
            .getId()
            .equals(currentUser.getId())) {

        throw new UnauthorizedException(
                "Only creator can update expense"
        );
    }

    expense.setTitle(request.title());
    expense.setDescription(request.description());
    expense.setCategory(request.category());
    expense.setAmount(request.amount());

    Expense saved =
            expenseRepo.save(expense);

    return new ExpenseResponse(
            saved.getId(),
            saved.getTitle(),
            saved.getDescription(),
            saved.getCategory(),
            saved.getAmount(),
            saved.getPaidBy().getId(),
            saved.getCreatedAt()
    );
}
   @Transactional
public void deleteExpense(UUID expenseId) {

    User currentUser =
            currentUserService.getCurrentUser();

    Expense expense = expenseRepo.findById(expenseId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Expense not found"
                    )
            );

    if (!expense.getPaidBy()
            .getId()
            .equals(currentUser.getId())) {

        throw new UnauthorizedException(
                "Only creator can delete expense"
        );
    }

    expenseSplitRepo.deleteByExpense(expense);

    expenseRepo.delete(expense);
}
 public List<BalanceResponse> getBalances(
        UUID tripId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(
            trip,
            currentUser
    )) {

        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    Map<String, BigDecimal> balances =
            new HashMap<>();

    List<ExpenseSplit> splits =
            expenseSplitRepo.findByExpenseTrip(
                    trip
            );

    for (ExpenseSplit split : splits) {

        Expense expense =
                split.getExpense();

        User payer =
                expense.getPaidBy();

        User participant =
                split.getUser();

        if (payer.getId().equals(
                participant.getId()
        )) {
            continue;
        }

        String key =
                participant.getId()
                + "_"
                + payer.getId();

        balances.merge(
                key,
                split.getSplitAmount(),
                BigDecimal::add
        );
    }

    List<Settlement> settlements =
            settlementRepo.findByTrip(trip);

    for (Settlement settlement : settlements) {

        String key =
                settlement.getPaidBy().getId()
                + "_"
                + settlement.getPaidTo().getId();

        balances.merge(
                key,
                settlement.getAmount().negate(),
                BigDecimal::add
        );
    }

    List<BalanceResponse> result =
            new ArrayList<>();

    for (Map.Entry<String, BigDecimal> entry :
            balances.entrySet()) {

        if (entry.getValue()
                .compareTo(BigDecimal.ZERO) <= 0) {

            continue;
        }

        String[] ids =
                entry.getKey().split("_");

        UUID fromId =
                UUID.fromString(ids[0]);

        UUID toId =
                UUID.fromString(ids[1]);

        User fromUser =
                userRepo.findById(fromId)
                        .orElseThrow();

        User toUser =
                userRepo.findById(toId)
                        .orElseThrow();

        result.add(
                new BalanceResponse(
                        fromId,
                        fromUser.getName(),
                        toId,
                        toUser.getName(),
                        entry.getValue()
                )
        );
    }

    return result;
}
@Transactional
public SettlementResponse createSettlement(
        UUID tripId,
        CreateSettlementRequest request
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(
            trip,
            currentUser
    )) {

        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    User paidTo =
            userRepo.findById(
                    request.paidToUserId()
            )
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "User not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(
            trip,
            paidTo
    )) {

        throw new IllegalArgumentException(
                "Target user is not a trip member"
        );
    }

    if (currentUser.getId().equals(
            paidTo.getId()
    )) {

        throw new IllegalArgumentException(
                "Cannot settle with yourself"
        );
    }

    Settlement settlement =
            new Settlement(
                    trip,
                    currentUser,
                    paidTo,
                    request.amount()
            );

    Settlement saved =
            settlementRepo.save(
                    settlement
            );

    return new SettlementResponse(
            saved.getId(),
            saved.getPaidBy().getId(),
            saved.getPaidTo().getId(),
            saved.getAmount(),
            saved.getCreatedAt()
    );
}
public List<SettlementResponse> getSettlements(
        UUID tripId
) {

    User currentUser =
            currentUserService.getCurrentUser();

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(
            trip,
            currentUser
    )) {

        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    return settlementRepo
            .findByTripOrderByCreatedAtDesc(
                    trip
            )
            .stream()
            .map(settlement ->
                    new SettlementResponse(
                            settlement.getId(),
                            settlement.getPaidBy().getId(),
                            settlement.getPaidTo().getId(),
                            settlement.getAmount(),
                            settlement.getCreatedAt()
                    )
            )
            .toList();
}
public List<SettlementSuggestionResponse>
getSettlementSuggestions(UUID tripId) {

    List<BalanceResponse> balances =
            getBalances(tripId);

    Map<UUID, BigDecimal> net =
            new HashMap<>();

    for (BalanceResponse balance : balances) {

        net.merge(
                balance.fromUserId(),
                balance.amount().negate(),
                BigDecimal::add
        );

        net.merge(
                balance.toUserId(),
                balance.amount(),
                BigDecimal::add
        );
    }

    List<Map.Entry<UUID, BigDecimal>> debtors =
            new ArrayList<>();

    List<Map.Entry<UUID, BigDecimal>> creditors =
            new ArrayList<>();

    for (Map.Entry<UUID, BigDecimal> entry :
            net.entrySet()) {

        if (entry.getValue()
                .compareTo(BigDecimal.ZERO) < 0) {

            debtors.add(entry);
        }

        if (entry.getValue()
                .compareTo(BigDecimal.ZERO) > 0) {

            creditors.add(entry);
        }
    }

    List<SettlementSuggestionResponse> result =
            new ArrayList<>();

    int i = 0;
    int j = 0;

    while (i < debtors.size()
            && j < creditors.size()) {

        UUID debtorId =
                debtors.get(i).getKey();

        UUID creditorId =
                creditors.get(j).getKey();

        BigDecimal debt =
                debtors.get(i)
                        .getValue()
                        .abs();

        BigDecimal credit =
                creditors.get(j)
                        .getValue();

        BigDecimal settle =
                debt.min(credit);

        User debtor =
                userRepo.findById(debtorId)
                        .orElseThrow();

        User creditor =
                userRepo.findById(creditorId)
                        .orElseThrow();

        result.add(
                new SettlementSuggestionResponse(
                        debtorId,
                        debtor.getName(),
                        creditorId,
                        creditor.getName(),
                        settle
                )
        );

        debtors.get(i).setValue(
                debtors.get(i)
                        .getValue()
                        .add(settle)
        );

        creditors.get(j).setValue(
                creditors.get(j)
                        .getValue()
                        .subtract(settle)
        );

        if (debtors.get(i)
                .getValue()
                .compareTo(BigDecimal.ZERO) == 0) {
            i++;
        }

        if (creditors.get(j)
                .getValue()
                .compareTo(BigDecimal.ZERO) == 0) {
            j++;
        }
    }

    return result;
}
}
