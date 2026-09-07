package com.tripnext.demo.repo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.Expense;
import com.tripnext.demo.model.Trip;

public interface ExpenseRepo extends JpaRepository<Expense, UUID> {
    List<Expense> findByTrip(Trip trip);
    Page<Expense> findByTripOrderByCreatedAtDesc(
        Trip trip,
        Pageable pageable
);
    Optional<Expense> findById(UUID id);
    Page<Expense> findByTrip(
        Trip trip,
        Pageable pageable
);
}
