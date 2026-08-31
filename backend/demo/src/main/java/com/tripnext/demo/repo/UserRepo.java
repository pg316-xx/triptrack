package com.tripnext.demo.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


import com.tripnext.demo.model.User;

public interface UserRepo extends JpaRepository<User, UUID>{
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
