package com.tripnext.demo.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.TripInvitation;

public interface TripInvitationRepo extends JpaRepository<TripInvitation, UUID>{
    
}
