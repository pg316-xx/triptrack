package com.tripnext.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.tripnext.demo.dto.expense.BalanceResponse;
import com.tripnext.demo.dto.expense.CreateExpenseRequest;
import com.tripnext.demo.dto.expense.CreateSettlementRequest;
import com.tripnext.demo.dto.expense.ExpenseResponse;
import com.tripnext.demo.dto.expense.ExpenseSplitRequest;
import com.tripnext.demo.dto.expense.ExpenseSplitResponse;
import com.tripnext.demo.dto.expense.SettlementResponse;
import com.tripnext.demo.dto.expense.SettlementSuggestionResponse;
import com.tripnext.demo.dto.expense.UpdateExpenseRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ConflictException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.Expense;
import com.tripnext.demo.model.help.NotificationType;
import com.tripnext.demo.model.ExpenseSplit;
import com.tripnext.demo.model.Settlement;
import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;
import com.tripnext.demo.repo.ExpenseRepo;
import com.tripnext.demo.repo.ExpenseSplitRepo;
import com.tripnext.demo.repo.SettlementRepo;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.TripRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.util.SplitCalculator;

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
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    @CacheEvict(value = "tripExpenses", key = "#tripId")
public ExpenseResponse createExpense(
        UUID tripId,
        CreateExpenseRequest request,
        String idempotencyKey
) {

    User currentUser =
            currentUserService.getCurrentUser();

    // A retried request with the same key returns the expense created the
    // first time instead of logging it twice.
    return idempotencyService.execute(
            "expense",
            currentUser.getId(),
            idempotencyKey,
            () -> doCreateExpense(tripId, request, currentUser),
            ExpenseResponse::id,
            this::replayExpense
    );
}

private ExpenseResponse doCreateExpense(
        UUID tripId,
        CreateExpenseRequest request,
        User currentUser
) {

    Trip trip = tripRepo.findById(tripId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Trip not found"
                    )
            );

    if (!tripMemberRepo.existsByTripAndUser(trip, currentUser)) {
        throw new UnauthorizedException(
                "You are not a member of this trip"
        );
    }

    validateExpenseFields(request.title(), request.amount());

    Map<User, BigDecimal> shares = resolveShares(
            trip,
            request.amount(),
            request.participantIds(),
            request.splits()
    );

    Expense savedExpense = expenseRepo.save(
            new Expense(
                    trip,
                    currentUser,
                    request.amount(),
                    request.title(),
                    request.description(),
                    request.category()
            )
    );

    List<ExpenseSplit> splits = saveSplits(savedExpense, shares);

    for (User participant : shares.keySet()) {
        if (!participant.getId().equals(currentUser.getId())) {
            notificationService.createNotification(
                    participant,
                    trip,
                    NotificationType.EXPENSE_ADDED,
                    "New Expense Added",
                    currentUser.getName() + " added an expense: " + request.title(),
                    "/trips/" + tripId + "/expenses"
            );
        }
    }

    return toResponse(savedExpense, splits);
}

private ExpenseResponse replayExpense(UUID expenseId) {
    Expense expense = expenseRepo.findById(expenseId)
            .orElseThrow(() ->
                    new ConflictException(
                            "This Idempotency-Key was already used for an expense that no longer exists"
                    )
            );
    return toResponse(expense, expenseSplitRepo.findByExpense(expense));
}

    // Not cached: key ignored the Pageable and skipped the membership check on cache hits,
    // and Page<record> does not round-trip through the Redis JSON serializer.
@Transactional
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

    Page<Expense> page = expenseRepo.findByTripOrderByCreatedAtDesc(
            trip,
            pageable
    );

    // One query for all splits on this page instead of one per expense.
    Map<UUID, List<ExpenseSplit>> splitsByExpense = page.isEmpty()
            ? Map.of()
            : expenseSplitRepo.findByExpenseIn(page.getContent())
                    .stream()
                    .collect(Collectors.groupingBy(s -> s.getExpense().getId()));

    return page.map(expense -> toResponse(
            expense,
            splitsByExpense.getOrDefault(expense.getId(), List.of())
    ));
}
    @Transactional
    @CacheEvict(value = "tripExpenses", allEntries = true)
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

    validateExpenseFields(request.title(), request.amount());

    boolean newSplitsGiven = notEmpty(request.participantIds()) || notEmpty(request.splits());
    boolean amountChanged = expense.getAmount().compareTo(request.amount()) != 0;

    List<ExpenseSplit> splits = expenseSplitRepo.findByExpense(expense);

    if (newSplitsGiven || amountChanged) {
        // Re-split: either as requested, or equally among the current
        // participants, so the splits keep adding up to the new amount.
        List<UUID> participantIds = newSplitsGiven
                ? request.participantIds()
                : splits.stream().map(s -> s.getUser().getId()).toList();

        Map<User, BigDecimal> shares = resolveShares(
                expense.getTrip(),
                request.amount(),
                participantIds,
                newSplitsGiven ? request.splits() : null
        );

        expenseSplitRepo.deleteByExpense(expense);
        expenseSplitRepo.flush();
        splits = saveSplits(expense, shares);
    }

    expense.setTitle(request.title());
    expense.setDescription(request.description());
    expense.setCategory(request.category());
    expense.setAmount(request.amount());

    Expense saved =
            expenseRepo.save(expense);

    auditLogService.logAction(
            saved.getTrip(),
            "UPDATE",
            "Expense",
            saved.getId(),
            "Expense details updated"
    );

    return toResponse(saved, splits);
}

private void validateExpenseFields(String title, BigDecimal amount) {
    if (title == null || title.isBlank()) {
        throw new BadRequestException(
                "Title is required"
        );
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException(
                "Amount must be greater than zero"
        );
    }
    if (!SplitCalculator.hasValidScale(amount)) {
        throw new BadRequestException(
                "Amount can have at most 2 decimal places"
        );
    }
}

/**
 * Works out who owes what. Exactly one of participantIds (equal split) or
 * splits (custom amounts) must be given; custom amounts must add up to the
 * total exactly (SRS FR-EXP-003). Every participant must be a trip member.
 */
private Map<User, BigDecimal> resolveShares(
        Trip trip,
        BigDecimal amount,
        List<UUID> participantIds,
        List<ExpenseSplitRequest> customSplits
) {
    boolean equal = notEmpty(participantIds);
    boolean custom = notEmpty(customSplits);

    if (equal == custom) {
        throw new BadRequestException(
                "Send either participantIds (equal split) or splits (custom amounts)"
        );
    }

    List<UUID> userIds = equal
            ? participantIds
            : customSplits.stream().map(ExpenseSplitRequest::userId).toList();

    if (userIds.contains(null)) {
        throw new BadRequestException("Participant id is required");
    }
    if (new HashSet<>(userIds).size() != userIds.size()) {
        throw new BadRequestException(
                "Duplicate participants are not allowed"
        );
    }

    List<BigDecimal> amounts;
    if (equal) {
        amounts = SplitCalculator.equalSplit(amount, userIds.size());
    } else {
        amounts = customSplits.stream().map(ExpenseSplitRequest::amount).toList();
        for (BigDecimal share : amounts) {
            if (share == null || share.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException(
                        "Each split amount must be greater than zero"
                );
            }
            if (!SplitCalculator.hasValidScale(share)) {
                throw new BadRequestException(
                        "Split amounts can have at most 2 decimal places"
                );
            }
        }
        BigDecimal sum = SplitCalculator.sum(amounts);
        if (sum.compareTo(amount) != 0) {
            throw new BadRequestException(
                    "INVALID_SPLIT_SUM: splits add up to " + sum
                    + " but the expense amount is " + amount.setScale(2)
            );
        }
    }

    Map<User, BigDecimal> shares = new LinkedHashMap<>();
    for (int i = 0; i < userIds.size(); i++) {
        User participant = userRepo.findById(userIds.get(i))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
        if (!tripMemberRepo.existsByTripAndUser(trip, participant)) {
            throw new BadRequestException(
                    "Participant is not a trip member"
            );
        }
        shares.put(participant, amounts.get(i));
    }
    return shares;
}

private List<ExpenseSplit> saveSplits(Expense expense, Map<User, BigDecimal> shares) {
    List<ExpenseSplit> splits = new ArrayList<>();
    shares.forEach((user, share) ->
            splits.add(expenseSplitRepo.save(new ExpenseSplit(expense, user, share)))
    );
    return splits;
}

private ExpenseResponse toResponse(Expense expense, List<ExpenseSplit> splits) {
    return new ExpenseResponse(
            expense.getId(),
            expense.getTitle(),
            expense.getDescription(),
            expense.getCategory(),
            expense.getAmount(),
            expense.getPaidBy().getId(),
            expense.getCreatedAt(),
            splits.stream()
                    .map(s -> new ExpenseSplitResponse(
                            s.getUser().getId(),
                            s.getUser().getName(),
                            s.getSplitAmount()
                    ))
                    .toList()
    );
}

private static boolean notEmpty(List<?> list) {
    return list != null && !list.isEmpty();
}
    @Transactional
    @CacheEvict(value = "tripExpenses", allEntries = true)
public void deleteExpense(
        UUID expenseId
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
                "Only creator can delete expense"
        );
    }

    expenseSplitRepo.deleteByExpense(expense);
    expenseRepo.delete(expense);

    auditLogService.logAction(
            expense.getTrip(),
            "DELETE",
            "Expense",
            expenseId,
            "Deleted expense: " + expense.getTitle()
    );
}
@Transactional
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
        CreateSettlementRequest request,
        String idempotencyKey
) {

    User currentUser =
            currentUserService.getCurrentUser();

    return idempotencyService.execute(
            "settlement",
            currentUser.getId(),
            idempotencyKey,
            () -> doCreateSettlement(tripId, request, currentUser),
            SettlementResponse::id,
            this::replaySettlement
    );
}

private SettlementResponse doCreateSettlement(
        UUID tripId,
        CreateSettlementRequest request,
        User currentUser
) {

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

    if (request.paidToUserId() == null) {
        throw new BadRequestException("paidToUserId is required");
    }
    if (request.amount() == null
            || request.amount().compareTo(BigDecimal.ZERO) <= 0
            || !SplitCalculator.hasValidScale(request.amount())) {
        throw new BadRequestException(
                "Amount must be greater than zero with at most 2 decimal places"
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

        throw new BadRequestException(
                "Target user is not a trip member"
        );
    }

    if (currentUser.getId().equals(
            paidTo.getId()
    )) {

        throw new BadRequestException(
                "Cannot settle with yourself"
        );
    }

    Settlement saved =
            settlementRepo.save(
                    new Settlement(
                            trip,
                            currentUser,
                            paidTo,
                            request.amount()
                    )
            );

    return toSettlementResponse(saved);
}

private SettlementResponse replaySettlement(UUID settlementId) {
    return settlementRepo.findById(settlementId)
            .map(this::toSettlementResponse)
            .orElseThrow(() ->
                    new ConflictException(
                            "This Idempotency-Key was already used"
                    )
            );
}

private SettlementResponse toSettlementResponse(Settlement saved) {
    return new SettlementResponse(
            saved.getId(),
            saved.getPaidBy().getId(),
            saved.getPaidTo().getId(),
            saved.getAmount(),
            saved.getCreatedAt()
    );
}
@Transactional
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
@Transactional
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
