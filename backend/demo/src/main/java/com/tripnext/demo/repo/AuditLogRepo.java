package com.tripnext.demo.repo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.AuditLog;
import com.tripnext.demo.model.Trip;

public interface AuditLogRepo extends JpaRepository<AuditLog, UUID> {
    
    Page<AuditLog> findByTripOrderByCreatedAtDesc(Trip trip, Pageable pageable);
}
