package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.Trip;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.WalletItem;

public interface WalletItemRepo extends JpaRepository<WalletItem, UUID> {

    List<WalletItem> findByUserOrderByCreatedAtDesc(User user);

    List<WalletItem> findByTripAndSharedTrueOrderByCreatedAtDesc(Trip trip);

    boolean existsByStorageKey(String storageKey);
}
