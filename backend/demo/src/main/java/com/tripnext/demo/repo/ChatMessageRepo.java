package com.tripnext.demo.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tripnext.demo.model.ChatMessage;
import com.tripnext.demo.model.Trip;

public interface ChatMessageRepo
        extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByTripOrderByCreatedAtAsc(
            Trip trip
    );

    Page<ChatMessage> findByTripOrderByCreatedAtDesc(
            Trip trip,
            Pageable pageable
    );
}
