package com.tripnext.demo.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Expensesplit;


public interface ExpensesplitRepo extends JpaRepository<Expensesplit, UUID>{
    
}
