package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Expense;
import com.tripnext.demo.model.ExpenseSplit;
import com.tripnext.demo.model.Trip;


public interface ExpenseSplitRepo extends JpaRepository<ExpenseSplit, UUID>{
    List<ExpenseSplit> findByExpense(Expense expense);
    void deleteByExpense(Expense expense);
    List<ExpenseSplit> findByExpenseTrip(Trip trip);
    List<ExpenseSplit> findByExpenseIn(List<Expense> expenses);
}
